package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface RejeitarPedidoUseCase {

    void executar(Visualizador visualizador, String seguirId);
}
