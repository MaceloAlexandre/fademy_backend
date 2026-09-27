package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.domain.conteudo.Post;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.UUID;

@Component
public class PostgresPostGateway implements PostGateway {

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
}
