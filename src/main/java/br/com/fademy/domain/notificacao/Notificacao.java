package br.com.fademy.domain.notificacao;

import br.com.fademy.domain.excecao.AcessoNegadoException;

import java.time.Instant;

public record Notificacao(String id, String destinatarioId, String atorId, TipoNotificacao tipo, String refId,
                          boolean lida, Instant criadaEm) {

    public static Notificacao nova(TipoNotificacao tipo, String destinatarioId, String atorId, String refId,
                                   Instant agora) {
        return new Notificacao(null, destinatarioId, atorId, tipo, refId, false, agora);
    }

    public void garantirDestinatario(String usuarioId) {
        if (!destinatarioId.equals(usuarioId)) {
            throw new AcessoNegadoException("Forbidden");
        }
    }
}
