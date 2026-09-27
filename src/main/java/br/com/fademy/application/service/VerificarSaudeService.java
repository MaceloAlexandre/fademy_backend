package br.com.fademy.application.service;

import br.com.fademy.application.gateway.SaudeGateway;
import br.com.fademy.application.usecase.VerificarSaudeUseCase;

public class VerificarSaudeService implements VerificarSaudeUseCase {

    private final SaudeGateway postgres;
    private final SaudeGateway mongo;

    public VerificarSaudeService(SaudeGateway postgres, SaudeGateway mongo) {
        this.postgres = postgres;
        this.mongo = mongo;
    }

    @Override
    public Saude executar() {
        return new Saude(postgres.estaNoAr(), mongo.estaNoAr());
    }
}
