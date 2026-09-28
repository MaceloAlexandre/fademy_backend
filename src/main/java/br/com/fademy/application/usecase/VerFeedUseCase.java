package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface VerFeedUseCase {

    Pagina<PostDetalhado> executar(Visualizador visualizador, String cursor, String limite);
}
