package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

/**
 * Comentar num post e listar os comentários (SPEC RN32, RN33).
 */
public interface ComentarUseCase {

    ComentarioDetalhado comentar(Visualizador visualizador, String postId, String texto);

    List<ComentarioDetalhado> listar(Visualizador visualizador, String postId);
}
