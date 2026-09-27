package br.com.fademy.domain.instituicao;

import java.util.Locale;

public enum TipoCategoria {

    CONTENT("content"),
    AREA("area");

    private final String texto;

    TipoCategoria(String texto) {
        this.texto = texto;
    }

    /** Como o tipo aparece na API e no banco. */
    public String texto() {
        return texto;
    }

    public static TipoCategoria deTexto(String texto) {
        return valueOf(texto.toUpperCase(Locale.ROOT));
    }
}
