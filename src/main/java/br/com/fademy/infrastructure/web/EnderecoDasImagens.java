package br.com.fademy.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monta a URL pública de uma imagem a partir da chave guardada no banco, com o
 * IMAGENS_URL_PUBLICA desta execução (a rota GET /api/media da própria API, #62). Assim, trocar
 * o endereço (outro IP na apresentação) não quebra as fotos que já existiam (#53).
 */
@Component
public class EnderecoDasImagens {

    private final String base;

    public EnderecoDasImagens(@Value("${fademy.imagens.url-publica}") String base) {
        this.base = base;
    }

    /** URL da imagem, ou null se não há imagem. */
    public String url(String chave) {
        return chave == null ? null : base + "/" + chave;
    }

    public List<String> urls(List<String> chaves) {
        return chaves.stream().map(this::url).toList();
    }
}
