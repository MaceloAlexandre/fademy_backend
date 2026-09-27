package br.com.fademy.domain.notificacao;

import java.time.Instant;

/**
 * Aviso para um usuário sobre algo que outro usuário (o ator) fez (SPEC RN34).
 */
public record Notificacao(String id, String destinatarioId, String atorId, TipoNotificacao tipo, String refId,
                          boolean lida, Instant criadaEm) {

    /** Notificação nova, ainda não lida. */
    public static Notificacao nova(TipoNotificacao tipo, String destinatarioId, String atorId, String refId,
                                   Instant agora) {
        return new Notificacao(null, destinatarioId, atorId, tipo, refId, false, agora);
    }
}
