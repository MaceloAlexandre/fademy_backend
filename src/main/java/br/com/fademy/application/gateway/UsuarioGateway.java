package br.com.fademy.application.gateway;

import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.Optional;

public interface UsuarioGateway {

    Optional<Usuario> buscarPorId(String id);

    /** O usuário de quem faz a requisição; se ele não existe mais, o token não vale. */
    default Usuario doVisualizador(Visualizador visualizador) {
        return buscarPorId(visualizador.id())
                .orElseThrow(() -> new NaoAutenticadoException("Invalid or expired token"));
    }

    /** Busca pelo e-mail, sem diferenciar maiúsculas. */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Grava um usuário novo e o devolve com id.
     *
     * @throws br.com.fademy.domain.excecao.ConflitoException se o e-mail já existe (SPEC RN09)
     */
    Usuario criar(Usuario usuario);
}
