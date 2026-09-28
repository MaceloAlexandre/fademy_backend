package br.com.fademy.application.gateway;

import br.com.fademy.domain.midia.Imagem;

/**
 * Armazenamento de imagens compatível com S3.
 */
public interface ArmazenamentoGateway {

    /** Grava a imagem na chave. O banco guarda só a chave; a URL é montada na resposta. */
    void enviar(String chave, Imagem imagem);

    boolean existe(String chave);
}
