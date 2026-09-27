package br.com.fademy.application.gateway;

import br.com.fademy.domain.conteudo.Comentario;

import java.util.List;

public interface ComentarioGateway {

    /** Grava o comentário e o devolve com id. */
    Comentario criar(Comentario comentario);

    /** Comentários do post, do mais antigo para o mais novo. */
    List<Comentario> doPost(String postId);
}
