package br.com.fademy.application.gateway;

import br.com.fademy.domain.convite.Convite;

import java.util.Optional;

public interface ConviteGateway {

    /** Grava um convite novo e o devolve com id. */
    Convite criar(Convite convite);

    Optional<Convite> buscarPorToken(String token);

    void atualizarStatus(Convite convite);

    /**
     * Consome uma vaga de forma atômica: dois cadastros simultâneos nunca passam de maxUsos. Na
     * última vaga, o convite passa a "expired" (SPEC RN13).
     *
     * @return false se não havia mais vaga (ou o convite deixou de valer) no momento da gravação
     */
    boolean consumirVaga(String conviteId);
}
