package br.com.fademy.application.gateway;

import br.com.fademy.domain.convite.Convite;

import java.util.Optional;

public interface ConviteGateway {

    Convite criar(Convite convite);

    Optional<Convite> buscarPorToken(String token);

    void atualizarStatus(Convite convite);

    boolean consumirVaga(String conviteId);
}
