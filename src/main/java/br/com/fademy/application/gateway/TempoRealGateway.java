package br.com.fademy.application.gateway;

import br.com.fademy.domain.conversa.Mensagem;

/**
 * Avisos em tempo real para quem está conectado (SPEC RN43).
 */
public interface TempoRealGateway {

    /** Entrega a mensagem nova a todas as conexões do destinatário. */
    void novaMensagem(String destinatarioId, Mensagem mensagem);
}
