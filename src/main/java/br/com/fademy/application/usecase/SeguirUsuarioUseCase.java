package br.com.fademy.application.usecase;

import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.usuario.Visualizador;

/**
 * Seguir um usuário (SPEC RN21).
 */
public interface SeguirUsuarioUseCase {

    Seguir executar(Visualizador visualizador, String alvoId);
}
