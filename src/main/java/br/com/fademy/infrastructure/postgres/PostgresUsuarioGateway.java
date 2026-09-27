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
                    rs.getString("avatar_url"),
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
    public Optional<Usuario> buscarPorEmail(String email) {
        return jdbc.sql("select * from usuario where lower(email) = lower(:email)")
                .param("email", email)
                .query(USUARIO)
                .optional();
    }

    @Override
    public void atualizar(Usuario usuario) {
        jdbc.sql("update usuario set privado = :privado, avatar_url = :avatarUrl where id = :id")
                .param("privado", usuario.privado())
                .param("avatarUrl", usuario.avatarUrl())
                .param("id", UUID.fromString(usuario.id()))
                .update();
    }

    @Override
    public Usuario criar(Usuario usuario) {
        UUID id = Ids.novo();
        try {
            jdbc.sql("""
                            insert into usuario (id, instituicao_id, nome, email, senha_hash, avatar_url,
                                                 papel, privado, ativo, criado_em)
                            values (:id, :instituicaoId, :nome, :email, :senhaHash, :avatarUrl,
                                    :papel, :privado, :ativo, :criadoEm)
                            """)
                    .param("id", id)
                    .param("instituicaoId", UUID.fromString(usuario.instituicaoId()))
                    .param("nome", usuario.nome())
                    .param("email", usuario.email())
                    .param("senhaHash", usuario.senhaHash())
                    .param("avatarUrl", usuario.avatarUrl())
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
                usuario.avatarUrl(), usuario.privado(), usuario.ativo(), usuario.criadoEm()));
    }
}
