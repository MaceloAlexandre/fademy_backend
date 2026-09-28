package br.com.fademy.application.gateway;

import br.com.fademy.domain.midia.Imagem;

import java.util.Optional;

public interface ArmazenamentoGateway {

    void enviar(String chave, Imagem imagem);

    boolean existe(String chave);

    Optional<Imagem> ler(String chave);
}
