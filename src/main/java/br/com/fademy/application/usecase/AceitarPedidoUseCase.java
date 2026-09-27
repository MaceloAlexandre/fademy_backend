package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * O dono do perfil aceita um pedido para segui-lo (SPEC RN23).
 */
public interface AceitarPedidoUseCase {

    void executar(Visualizador visualizador, String seguirId);
}
