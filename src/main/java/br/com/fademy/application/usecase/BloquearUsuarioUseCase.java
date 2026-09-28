package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface BloquearUsuarioUseCase {

    void executar(Visualizador visualizador, String alvoId);
}
