package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.ComentarioGateway;
import br.com.fademy.domain.conteudo.Comentario;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
public class PostgresComentarioGateway implements ComentarioGateway {

    private final JdbcClient jdbc;

    public PostgresComentarioGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Comentario criar(Comentario c) {
        UUID id = Ids.novo();
        jdbc.sql("""
                        insert into comentario (id, post_id, autor_id, instituicao_id, texto, criado_em)
                        values (:id, :post, :autor, :instituicao, :texto, :criadoEm)
                        """)
                .param("id", id)
                .param("post", UUID.fromString(c.postId()))
                .param("autor", UUID.fromString(c.autorId()))
                .param("instituicao", UUID.fromString(c.instituicaoId()))
                .param("texto", c.texto())
                .param("criadoEm", Timestamp.from(c.criadoEm()))
                .update();
        return new Comentario(id.toString(), c.postId(), c.autorId(), c.instituicaoId(), c.texto(), c.criadoEm());
    }

    @Override
    public List<Comentario> doPost(String postId) {
        return jdbc.sql("select * from comentario where post_id = :post order by id")
                .param("post", UUID.fromString(postId))
                .query((rs, n) -> new Comentario(rs.getString("id"), rs.getString("post_id"), rs.getString("autor_id"),
                        rs.getString("instituicao_id"), rs.getString("texto"), rs.getTimestamp("criado_em").toInstant()))
                .list();
    }
}
