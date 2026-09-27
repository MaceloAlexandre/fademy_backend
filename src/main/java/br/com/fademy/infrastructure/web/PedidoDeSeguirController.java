package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.AceitarPedidoUseCase;
import br.com.fademy.application.usecase.RejeitarPedidoUseCase;
import br.com.fademy.domain.usuario.Visualizador;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PedidoDeSeguirController {

    private final AceitarPedidoUseCase aceitarPedido;
    private final RejeitarPedidoUseCase rejeitarPedido;

    public PedidoDeSeguirController(AceitarPedidoUseCase aceitarPedido, RejeitarPedidoUseCase rejeitarPedido) {
        this.aceitarPedido = aceitarPedido;
        this.rejeitarPedido = rejeitarPedido;
    }

    /** SPEC RN23. */
    @PatchMapping("/api/follows/{id}/accept")
    public AceitoResponse aceitar(Visualizador visualizador, @PathVariable String id) {
        aceitarPedido.executar(visualizador, id);
        return new AceitoResponse("accepted");
    }

    /** SPEC RN24. */
    @PatchMapping("/api/follows/{id}/reject")
    public RejeitadoResponse rejeitar(Visualizador visualizador, @PathVariable String id) {
        rejeitarPedido.executar(visualizador, id);
        return new RejeitadoResponse(true);
    }

    public record AceitoResponse(String status) {
    }

    public record RejeitadoResponse(boolean rejected) {
    }
}
