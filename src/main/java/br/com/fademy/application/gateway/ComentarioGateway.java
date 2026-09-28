package br.com.fademy.application.gateway;

import br.com.fademy.domain.conteudo.Comentario;

import java.util.List;

public interface ComentarioGateway {

    Comentario criar(Comentario comentario);

    List<Comentario> doPost(String postId);
}
