package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Usuario;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class PostgresPostGateway implements PostGateway {

    // Colunas do post mais os ids das categorias, numa linha só.
    static final String COLUNAS = """
            p.*, array(select pc.categoria_id::text from post_categoria pc where pc.post_id = p.id) as categorias
            """;

    static final RowMapper<Post> POST = (rs, n) -> new Post(
            rs.getString("id"),
            rs.getString("autor_id"),
            rs.getString("instituicao_id"),
            rs.getString("texto"),
            Arrays.asList((String[]) rs.getArray("imagens").getArray()),
            rs.getString("link_url"),
            Arrays.asList((String[]) rs.getArray("categorias").getArray()),
            rs.getInt("comentarios"),
            rs.getTimestamp("criado_em").toInstant());

    private final JdbcClient jdbc;

    public PostgresPostGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Post criar(Post p) {
        UUID id = Ids.novo();
        jdbc.sql("""
                        insert into post (id, autor_id, instituicao_id, texto, imagens, link_url, comentarios, criado_em)
                        values (:id, :autor, :instituicao, :texto, :imagens, :link, :comentarios, :criadoEm)
                        """)
                .param("id", id)
                .param("autor", UUID.fromString(p.autorId()))
                .param("instituicao", UUID.fromString(p.instituicaoId()))
                .param("texto", p.texto())
                .param("imagens", p.imagens().toArray(String[]::new))
                .param("link", p.linkUrl())
                .param("comentarios", p.comentarios())
                .param("criadoEm", Timestamp.from(p.criadoEm()))
                .update();
        for (String categoria : p.categoriaIds()) {
            jdbc.sql("insert into post_categoria (post_id, categoria_id) values (:post, :categoria)")
                    .param("post", id)
                    .param("categoria", UUID.fromString(categoria))
                    .update();
        }
        return new Post(id.toString(), p.autorId(), p.instituicaoId(), p.texto(), p.imagens(), p.linkUrl(),
                p.categoriaIds(), p.comentarios(), p.criadoEm());
    }

    @Override
    public List<Post> feed(Usuario leitor, String cursor, int quantidade) {
        return jdbc.sql("select " + COLUNAS + """
                        from post p
                        join seguir s on s.seguido_id = p.autor_id and s.seguidor_id = :eu and s.status = 'accepted'
                        where p.instituicao_id = :instituicao
                          and not exists (select 1 from bloqueio b
                                          where (b.bloqueador_id = :eu and b.bloqueado_id = p.autor_id)
                                             or (b.bloqueador_id = p.autor_id and b.bloqueado_id = :eu))
                          and (cast(:cursor as uuid) is null or p.id < :cursor)
                        order by p.id desc
                        limit :quantidade
                        """)
                .param("eu", UUID.fromString(leitor.id()))
                .param("instituicao", UUID.fromString(leitor.instituicaoId()))
                .param("cursor", cursor(cursor))
                .param("quantidade", quantidade)
                .query(POST)
                .list();
    }

    /** Cursor da SPEC 5.4: ausente é "desde o início"; formato inválido é erro do cliente. */
    static UUID cursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        return Ids.ler(cursor).orElseThrow(() -> new DadosInvalidosException("Invalid cursor"));
    }
}
