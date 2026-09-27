package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * Bloquear um usuário (SPEC RN25).
 */
public interface BloquearUsuarioUseCase {

    void executar(Visualizador visualizador, String alvoId);
}
