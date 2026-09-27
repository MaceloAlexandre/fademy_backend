package br.com.fademy.application.gateway;

import br.com.fademy.domain.conteudo.Post;

public interface PostGateway {

    /** Grava o post com suas categorias e o devolve com id. */
    Post criar(Post post);
}
