package br.com.fademy.application.gateway;

import br.com.fademy.domain.midia.Imagem;

import java.util.Optional;

/**
 * Armazenamento das imagens (no MongoDB, pelo GridFS).
 */
public interface ArmazenamentoGateway {

    /** Grava a imagem na chave. O banco guarda só a chave; a URL é montada na resposta. */
    void enviar(String chave, Imagem imagem);

    boolean existe(String chave);

    Optional<Imagem> ler(String chave);
}
