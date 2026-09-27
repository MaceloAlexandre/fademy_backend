package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * Posts de um usuário, respeitando bloqueio e perfil privado (SPEC RN31).
 */
public interface ListarPostsDoUsuarioUseCase {

    Pagina<PostDetalhado> executar(Visualizador visualizador, String autorId, String cursor, String limite);
}
