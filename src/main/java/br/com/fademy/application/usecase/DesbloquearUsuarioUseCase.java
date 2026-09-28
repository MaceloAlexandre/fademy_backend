package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface DesbloquearUsuarioUseCase {

    void executar(Visualizador visualizador, String alvoId);
}
