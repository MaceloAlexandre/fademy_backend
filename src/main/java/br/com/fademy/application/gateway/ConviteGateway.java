package br.com.fademy.application.gateway;

import br.com.fademy.domain.convite.Convite;

import java.util.Optional;

public interface ConviteGateway {

    /** Grava um convite novo e o devolve com id. */
    Convite criar(Convite convite);

    Optional<Convite> buscarPorToken(String token);

    void atualizarStatus(Convite convite);
}
