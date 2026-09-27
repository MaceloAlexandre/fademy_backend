package br.com.fademy.domain.notificacao;

/**
 * Tipos de notificação e o que cada uma referencia (SPEC RN34).
 */
public enum TipoNotificacao {

    NEW_FOLLOWER("new_follower", "Follow"),
    FOLLOW_REQUEST("follow_request", "Follow"),
    FOLLOW_ACCEPTED("follow_accepted", "Follow"),
    POST_COMMENT("post_comment", "Post"),
    INVITE_RECEIVED("invite_received", "User"),
    MESSAGE_RECEIVED("message_received", "Conversation");

    private final String texto;
    private final String modeloReferencia;

    TipoNotificacao(String texto, String modeloReferencia) {
        this.texto = texto;
        this.modeloReferencia = modeloReferencia;
    }

    /** Como o tipo aparece na API ({@code type}). */
    public String texto() {
        return texto;
    }

    /** O que {@code refId} referencia ({@code refModel} na API). */
    public String modeloReferencia() {
        return modeloReferencia;
    }

    public static TipoNotificacao deTexto(String texto) {
        for (TipoNotificacao tipo : values()) {
            if (tipo.texto.equals(texto)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de notificação desconhecido: " + texto);
    }
}
