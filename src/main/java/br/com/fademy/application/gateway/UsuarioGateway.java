package br.com.fademy.application.gateway;

import br.com.fademy.domain.usuario.Usuario;

import java.util.Optional;

public interface UsuarioGateway {

    /** Busca pelo e-mail, sem diferenciar maiúsculas. */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Grava um usuário novo e o devolve com id.
     *
     * @throws br.com.fademy.domain.excecao.ConflitoException se o e-mail já existe (SPEC RN09)
     */
    Usuario criar(Usuario usuario);
}
