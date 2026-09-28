package br.com.fademy.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnderecoDasImagens {

    private final String base;

    public EnderecoDasImagens(@Value("${fademy.imagens.url-publica}") String base) {
        this.base = base;
    }

    public String url(String chave) {
        return chave == null ? null : base + "/" + chave;
    }

    public List<String> urls(List<String> chaves) {
        return chaves.stream().map(this::url).toList();
    }
}
