package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * Desbloquear um usuário (SPEC RN26).
 */
public interface DesbloquearUsuarioUseCase {

    void executar(Visualizador visualizador, String alvoId);
}
