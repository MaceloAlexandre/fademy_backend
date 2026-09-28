package br.com.fademy.application.gateway;

import br.com.fademy.domain.conversa.Mensagem;

public interface TempoRealGateway {

    void novaMensagem(String destinatarioId, Mensagem mensagem);
}
