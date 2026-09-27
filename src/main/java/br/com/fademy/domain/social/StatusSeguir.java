package br.com.fademy.domain.social;

import java.util.Locale;

public enum StatusSeguir {

    PENDING, ACCEPTED;

    /** Como o status aparece na API e no banco. */
    public String texto() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static StatusSeguir deTexto(String texto) {
        return valueOf(texto.toUpperCase(Locale.ROOT));
    }
}
