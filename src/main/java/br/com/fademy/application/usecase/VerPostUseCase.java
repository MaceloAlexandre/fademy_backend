package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * Um post, se visível para quem pede (SPEC RN30).
 */
public interface VerPostUseCase {

    PostDetalhado executar(Visualizador visualizador, String postId);
}
