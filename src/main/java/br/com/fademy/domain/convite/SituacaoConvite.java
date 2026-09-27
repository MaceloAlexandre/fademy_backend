package br.com.fademy.domain.convite;

/**
 * Se o convite ainda pode ser usado e, se não, por quê (SPEC RN11).
 */
public enum SituacaoConvite {

    VALIDO(null),
    REVOGADO("revoked"),
    EXPIRADO("expired"),
    CHEIO("full");

    private final String motivo;

    SituacaoConvite(String motivo) {
        this.motivo = motivo;
    }

    public boolean valido() {
        return this == VALIDO;
    }

    /** Motivo como aparece na API ({@code reason}); nulo quando válido. */
    public String motivo() {
        return motivo;
    }
}
