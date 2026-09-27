package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.PostDetalhado;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Post com autor e categorias populados (SPEC 5.3).
 */
public record PostResponse(@JsonProperty("_id") String id, Autor authorId, String institutionId, String text,
                           List<String> imageUrls, @JsonInclude(JsonInclude.Include.NON_NULL) String linkUrl,
                           boolean hasLink, List<CategoriaResumo> categoryIds, int commentCount, Instant createdAt) {

    static PostResponse de(PostDetalhado d) {
        Post p = d.post();
        return new PostResponse(p.id(), Autor.de(d.autor()), p.instituicaoId(), p.texto(), p.imagens(), p.linkUrl(),
                p.temLink(), d.categorias().stream().map(CategoriaResumo::de).toList(), p.comentarios(),
                p.criadoEm());
    }

    public record Autor(@JsonProperty("_id") String id, String name, String avatarUrl) {

        static Autor de(Usuario u) {
            return u == null ? null : new Autor(u.id(), u.nome(), u.avatarUrl());
        }
    }

    public record CategoriaResumo(@JsonProperty("_id") String id, String name, String slug, String type) {

        static CategoriaResumo de(Categoria c) {
            return new CategoriaResumo(c.id(), c.nome(), c.slug(), c.tipo().texto());
        }
    }

    public record PaginaDePosts(List<PostResponse> posts, boolean hasMore, String nextCursor) {
    }
}
