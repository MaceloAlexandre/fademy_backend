package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.excecao.ConflitoException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresUsuarioGateway implements UsuarioGateway {

    // Linha do banco → subclasse de Usuario do papel gravado.
    private static final RowMapper<Usuario> USUARIO = (rs, n) -> Papel.deTexto(rs.getString("papel"))
            .criarUsuario(new DadosUsuario(
                    rs.getString("id"),
                    rs.getString("instituicao_id"),
                    rs.getString("nome"),
                    rs.getString("email"),
                    rs.getString("senha_hash"),
                    rs.getString("avatar_chave"),
                    rs.getBoolean("privado"),
                    rs.getBoolean("ativo"),
                    rs.getTimestamp("criado_em").toInstant()));

    private final JdbcClient jdbc;

    public PostgresUsuarioGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        return Ids.ler(id).flatMap(uuid -> jdbc.sql("select * from usuario where id = :id")
                .param("id", uuid)
                .query(USUARIO)
                .optional());
    }

    @Override
    public List<Usuario> buscarPorIds(Collection<String> ids) {
        List<UUID> validos = ids.stream().flatMap(id -> Ids.ler(id).stream()).toList();
        if (validos.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select * from usuario where id in (:ids)").param("ids", validos).query(USUARIO).list();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jdbc.sql("select * from usuario where lower(email) = lower(:email)")
                .param("email", email)
                .query(USUARIO)
                .optional();
    }

    // Mesma instituição, sem o visualizador e sem bloqueio em nenhum sentido (SPEC RN19, RN20).
    private static final String VISIVEIS = """
            u.instituicao_id = :instituicao and u.id <> :eu
            and not exists (select 1 from bloqueio b
                            where (b.bloqueador_id = :eu and b.bloqueado_id = u.id)
                               or (b.bloqueador_id = u.id and b.bloqueado_id = :eu))
            """;

    private static final String NOME_CONTEM =
            "unaccent(lower(u.nome)) like '%' || unaccent(lower(:trecho)) || '%' escape '\\'";

    @Override
    public List<Usuario> buscarPorNome(Usuario visualizador, String trecho, int deslocamento, int limite) {
        return jdbc.sql("select u.* from usuario u where " + VISIVEIS + " and " + NOME_CONTEM
                        + " order by u.nome, u.id limit :limite offset :deslocamento")
                .param("instituicao", UUID.fromString(visualizador.instituicaoId()))
                .param("eu", UUID.fromString(visualizador.id()))
                .param("trecho", escaparLike(trecho))
                .param("limite", limite)
                .param("deslocamento", deslocamento)
                .query(USUARIO)
                .list();
    }

    @Override
    public long contarPorNome(Usuario visualizador, String trecho) {
        return jdbc.sql("select count(*) from usuario u where " + VISIVEIS + " and " + NOME_CONTEM)
                .param("instituicao", UUID.fromString(visualizador.instituicaoId()))
                .param("eu", UUID.fromString(visualizador.id()))
                .param("trecho", escaparLike(trecho))
                .query(Long.class)
                .single();
    }

    @Override
    public List<Usuario> candidatosASugestao(Usuario visualizador) {
        return jdbc.sql("select u.* from usuario u where " + VISIVEIS + """
                         and not exists (select 1 from seguir s where s.seguidor_id = :eu and s.seguido_id = u.id)
                        order by u.criado_em desc, u.id desc
                        """)
                .param("instituicao", UUID.fromString(visualizador.instituicaoId()))
                .param("eu", UUID.fromString(visualizador.id()))
                .query(USUARIO)
                .list();
    }

    @Override
    public long contarDaInstituicao(String instituicaoId) {
        return jdbc.sql("select count(*) from usuario where instituicao_id = :instituicao")
                .param("instituicao", UUID.fromString(instituicaoId))
                .query(Long.class)
                .single();
    }

    @Override
    public List<Usuario> seguidoresMutuos(Usuario usuario) {
        return jdbc.sql("""
                        select u.* from usuario u
                        where exists (select 1 from seguir s
                                      where s.seguidor_id = :eu and s.seguido_id = u.id and s.status = 'accepted')
                          and exists (select 1 from seguir s
                                      where s.seguidor_id = u.id and s.seguido_id = :eu and s.status = 'accepted')
                        order by u.nome
                        """)
                .param("eu", UUID.fromString(usuario.id()))
                .query(USUARIO)
                .list();
    }

    // O trecho digitado é texto, não padrão: % e _ valem como caracteres comuns.
    private static String escaparLike(String trecho) {
        return trecho.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public void atualizar(Usuario usuario) {
        jdbc.sql("update usuario set privado = :privado, avatar_chave = :avatarChave where id = :id")
                .param("privado", usuario.privado())
                .param("avatarChave", usuario.avatarChave())
                .param("id", UUID.fromString(usuario.id()))
                .update();
    }

    @Override
    public Usuario criar(Usuario usuario) {
        UUID id = Ids.novo();
        try {
            jdbc.sql("""
                            insert into usuario (id, instituicao_id, nome, email, senha_hash, avatar_chave,
                                                 papel, privado, ativo, criado_em)
                            values (:id, :instituicaoId, :nome, :email, :senhaHash, :avatarChave,
                                    :papel, :privado, :ativo, :criadoEm)
                            """)
                    .param("id", id)
                    .param("instituicaoId", UUID.fromString(usuario.instituicaoId()))
                    .param("nome", usuario.nome())
                    .param("email", usuario.email())
                    .param("senhaHash", usuario.senhaHash())
                    .param("avatarChave", usuario.avatarChave())
                    .param("papel", usuario.papel().texto())
                    .param("privado", usuario.privado())
                    .param("ativo", usuario.ativo())
                    .param("criadoEm", Timestamp.from(usuario.criadoEm()))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new ConflitoException("Este e-mail já está em uso");
        }
        return usuario.papel().criarUsuario(new DadosUsuario(
                id.toString(), usuario.instituicaoId(), usuario.nome(), usuario.email(), usuario.senhaHash(),
                usuario.avatarChave(), usuario.privado(), usuario.ativo(), usuario.criadoEm()));
    }
}
