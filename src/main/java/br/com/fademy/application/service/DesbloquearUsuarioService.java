package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.usecase.DesbloquearUsuarioUseCase;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Visualizador;

public class DesbloquearUsuarioService implements DesbloquearUsuarioUseCase {

    private final BloqueioGateway bloqueios;

    public DesbloquearUsuarioService(BloqueioGateway bloqueios) {
        this.bloqueios = bloqueios;
    }

    @Override
    public void executar(Visualizador visualizador, String alvoId) {
        if (visualizador.id().equals(alvoId)) {
            throw new DadosInvalidosException("Cannot unblock yourself");
        }
        bloqueios.desbloquear(visualizador.id(), alvoId);
    }
}
