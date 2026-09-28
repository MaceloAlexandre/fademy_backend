package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface ListarPostsDoUsuarioUseCase {

    Pagina<PostDetalhado> executar(Visualizador visualizador, String autorId, String cursor, String limite);
}
