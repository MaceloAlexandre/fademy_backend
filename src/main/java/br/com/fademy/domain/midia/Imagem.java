package br.com.fademy.domain.midia;

import java.util.Map;
import java.util.UUID;

public record Imagem(byte[] conteudo, String tipo) {

    private static final Map<String, String> EXTENSOES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    public Imagem {
        if (!aceita(tipo)) {
            throw new IllegalArgumentException("Tipo de imagem não aceito: " + tipo);
        }
    }

    public static boolean aceita(String tipo) {
        return tipo != null && EXTENSOES.containsKey(tipo);
    }

    public String novaChave(String pasta) {
        return pasta + "/" + UUID.randomUUID() + "." + EXTENSOES.get(tipo);
    }
}
