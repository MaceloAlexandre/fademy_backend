package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * O dono do perfil rejeita um pedido para segui-lo (SPEC RN24).
 */
public interface RejeitarPedidoUseCase {

    void executar(Visualizador visualizador, String seguirId);
}
