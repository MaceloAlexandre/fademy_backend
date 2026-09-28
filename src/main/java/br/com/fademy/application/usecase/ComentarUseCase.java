package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public interface ComentarUseCase {

    ComentarioDetalhado comentar(Visualizador visualizador, String postId, String texto);

    List<ComentarioDetalhado> listar(Visualizador visualizador, String postId);
}
