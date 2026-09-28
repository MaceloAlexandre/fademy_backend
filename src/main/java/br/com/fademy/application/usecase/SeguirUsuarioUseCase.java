package br.com.fademy.application.usecase;

import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.usuario.Visualizador;

public interface SeguirUsuarioUseCase {

    Seguir executar(Visualizador visualizador, String alvoId);
}
