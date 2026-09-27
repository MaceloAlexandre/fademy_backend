package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * Revoga um convite da instituição de quem pede (SPEC RN08, RN12).
 */
public interface RevogarConviteUseCase {

    void executar(Visualizador visualizador, String token);
}
