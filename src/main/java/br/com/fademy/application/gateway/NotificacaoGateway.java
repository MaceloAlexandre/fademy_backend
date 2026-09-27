package br.com.fademy.application.gateway;

import br.com.fademy.domain.notificacao.Notificacao;

/**
 * Notificações no MongoDB.
 */
public interface NotificacaoGateway {

    /**
     * Grava a notificação. Notificação é efeito colateral e nunca derruba a ação principal
     * (SPEC RN37): em caso de falha, a implementação registra no log e não lança exceção.
     */
    void criar(Notificacao notificacao);
}
