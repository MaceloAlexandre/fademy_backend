package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.usuario.Usuario;

/**
 * Post que o leitor pode ver: da instituição dele e de um autor que não o bloqueou (SPEC RN30).
 * Usado por ver post, comentar e listar comentários.
 */
class PostsVisiveis {

    private final PostGateway posts;
    private final BloqueioGateway bloqueios;

    PostsVisiveis(PostGateway posts, BloqueioGateway bloqueios) {
        this.posts = posts;
        this.bloqueios = bloqueios;
    }

    Post buscar(Usuario leitor, String postId) {
        return posts.buscarPorId(postId)
                .filter(post -> post.daInstituicao(leitor.instituicaoId()))
                .filter(post -> !bloqueios.bloqueou(post.autorId(), leitor.id()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Post not found"));
    }
}
