package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.BloquearUsuarioUseCase;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public class BloquearUsuarioService implements BloquearUsuarioUseCase {

    private final UsuarioGateway usuarios;
    private final SeguirGateway seguires;
    private final BloqueioGateway bloqueios;
    private final NotificacaoGateway notificacoes;
    private final TransacaoGateway transacao;

    public BloquearUsuarioService(UsuarioGateway usuarios, SeguirGateway seguires, BloqueioGateway bloqueios,
                                  NotificacaoGateway notificacoes, TransacaoGateway transacao) {
        this.usuarios = usuarios;
        this.seguires = seguires;
        this.bloqueios = bloqueios;
        this.notificacoes = notificacoes;
        this.transacao = transacao;
    }

    @Override
    public void executar(Visualizador visualizador, String alvoId) {
        if (visualizador.id().equals(alvoId)) {
            throw new DadosInvalidosException("Cannot block yourself");
        }
        Usuario eu = usuarios.doVisualizador(visualizador);
        Usuario alvo = usuarios.buscarPorId(alvoId)
                .filter(eu::mesmaInstituicao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("User not found"));

        transacao.executar(() -> {
            seguires.removerEntre(eu.id(), alvo.id());
            bloqueios.bloquear(eu.id(), alvo.id());
        });
        notificacoes.removerPedidosDeSeguirEntre(eu.id(), alvo.id());
    }
}
