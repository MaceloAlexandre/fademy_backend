package br.com.fademy.application.service;

import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.usecase.LerNotificacaoUseCase;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.usuario.Visualizador;

public class LerNotificacaoService implements LerNotificacaoUseCase {

    private final NotificacaoGateway notificacoes;

    public LerNotificacaoService(NotificacaoGateway notificacoes) {
        this.notificacoes = notificacoes;
    }

    @Override
    public void marcarComoLida(Visualizador visualizador, String notificacaoId) {
        notificacoes.marcarComoLida(minha(visualizador, notificacaoId).id());
    }

    @Override
    public void excluir(Visualizador visualizador, String notificacaoId) {
        notificacoes.remover(minha(visualizador, notificacaoId).id());
    }

    private Notificacao minha(Visualizador visualizador, String notificacaoId) {
        Notificacao notificacao = notificacoes.buscarPorId(notificacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notification not found"));
        notificacao.garantirDestinatario(visualizador.id());
        return notificacao;
    }
}
