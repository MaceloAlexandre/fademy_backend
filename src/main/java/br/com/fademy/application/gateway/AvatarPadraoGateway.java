package br.com.fademy.application.gateway;

import java.util.Optional;

/**
 * Cria uma foto padrão para quem não enviou uma.
 */
public interface AvatarPadraoGateway {

    /**
     * Gera a foto a partir do nome e a grava no armazenamento.
     *
     * @return a chave da foto, ou vazio se não foi possível gravar (a falha vai para o log)
     */
    Optional<String> criar(String nome);
}
