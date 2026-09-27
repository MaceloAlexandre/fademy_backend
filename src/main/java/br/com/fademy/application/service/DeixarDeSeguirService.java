package br.com.fademy.application.service;

import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.usecase.DeixarDeSeguirUseCase;
import br.com.fademy.domain.usuario.Visualizador;

public class DeixarDeSeguirService implements DeixarDeSeguirUseCase {

    private final SeguirGateway seguires;

    public DeixarDeSeguirService(SeguirGateway seguires) {
        this.seguires = seguires;
    }

    @Override
    public void executar(Visualizador visualizador, String alvoId) {
        seguires.remover(visualizador.id(), alvoId);
    }
}
