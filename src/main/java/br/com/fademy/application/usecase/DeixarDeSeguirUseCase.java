package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * Deixar de seguir ou cancelar um pedido (SPEC RN22).
 */
public interface DeixarDeSeguirUseCase {

    void executar(Visualizador visualizador, String alvoId);
}
