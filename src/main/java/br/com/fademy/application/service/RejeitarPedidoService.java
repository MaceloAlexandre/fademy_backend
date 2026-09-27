package br.com.fademy.application.service;

import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.usecase.RejeitarPedidoUseCase;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.usuario.Visualizador;

public class RejeitarPedidoService implements RejeitarPedidoUseCase {

    private final SeguirGateway seguires;

    public RejeitarPedidoService(SeguirGateway seguires) {
        this.seguires = seguires;
    }

    @Override
    public void executar(Visualizador visualizador, String seguirId) {
        Seguir seguir = seguires.buscarPorId(seguirId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Follow request not found"));
        seguir.garantirQueEhOSeguido(visualizador.id());
        seguires.removerPorId(seguir.id());
    }
}
