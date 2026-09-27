package br.com.fademy.domain.convite;

import java.util.Locale;

public enum StatusConvite {

    ACTIVE, REVOKED, EXPIRED;

    /** Como o status aparece no banco. */
    public String texto() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static StatusConvite deTexto(String texto) {
        return valueOf(texto.toUpperCase(Locale.ROOT));
    }
}
