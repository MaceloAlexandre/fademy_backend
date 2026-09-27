package br.com.fademy.application.service;

import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.usecase.AceitarPedidoUseCase;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;

public class AceitarPedidoService implements AceitarPedidoUseCase {

    private final SeguirGateway seguires;
    private final NotificacaoGateway notificacoes;

    public AceitarPedidoService(SeguirGateway seguires, NotificacaoGateway notificacoes) {
        this.seguires = seguires;
        this.notificacoes = notificacoes;
    }

    @Override
    public void executar(Visualizador visualizador, String seguirId) {
        Seguir seguir = seguires.buscarPorId(seguirId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Follow request not found"));
        seguir.aceitarPor(visualizador.id());
        seguires.atualizarStatus(seguir);

        notificacoes.criar(Notificacao.nova(
                TipoNotificacao.FOLLOW_ACCEPTED, seguir.seguidorId(), seguir.seguidoId(), seguir.id(), Instant.now()));
    }
}
