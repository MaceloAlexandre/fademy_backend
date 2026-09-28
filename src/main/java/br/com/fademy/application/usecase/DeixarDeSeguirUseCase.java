package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface DeixarDeSeguirUseCase {

    void executar(Visualizador visualizador, String alvoId);
}
