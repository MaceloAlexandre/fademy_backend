package br.com.fademy.application.gateway;

import br.com.fademy.domain.midia.Imagem;

/**
 * Armazenamento de imagens compatível com S3.
 */
public interface ArmazenamentoGateway {

    /** Grava a imagem na chave e devolve a URL pública de leitura. */
    String enviar(String chave, Imagem imagem);

    boolean existe(String chave);
}
