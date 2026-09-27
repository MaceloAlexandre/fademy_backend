package br.com.fademy.application.gateway;

import br.com.fademy.domain.social.Seguir;

import java.util.Optional;

public interface SeguirGateway {

    Optional<Seguir> buscar(String seguidorId, String seguidoId);

    /**
     * Grava um follow novo e o devolve com id.
     *
     * @throws br.com.fademy.domain.excecao.ConflitoException se o par já existe
     */
    Seguir criar(Seguir seguir);

    /** Remove o follow do seguidor para o seguido, em qualquer status; sem follow, não faz nada. */
    void remover(String seguidorId, String seguidoId);
}
