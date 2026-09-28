package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface VerPostUseCase {

    PostDetalhado executar(Visualizador visualizador, String postId);
}
