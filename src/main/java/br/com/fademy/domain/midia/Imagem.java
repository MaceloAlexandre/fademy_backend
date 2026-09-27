package br.com.fademy.domain.midia;

import java.util.Map;
import java.util.UUID;

/**
 * Imagem enviada por um usuário: avatar, foto de post ou de mensagem (SPEC RN14, RN15).
 */
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

    /** Chave gerada pelo servidor dentro da pasta; o nome do arquivo do cliente nunca é usado. */
    public String novaChave(String pasta) {
        return pasta + "/" + UUID.randomUUID() + "." + EXTENSOES.get(tipo);
    }
}
