package br.com.fademy.domain.convite;

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

    public String motivo() {
        return motivo;
    }
}
