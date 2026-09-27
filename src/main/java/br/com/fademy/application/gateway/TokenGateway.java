package br.com.fademy.application.gateway;

import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public interface TokenGateway {

    String gerar(Usuario usuario);

    /**
     * @throws br.com.fademy.domain.excecao.NaoAutenticadoException se o token é inválido ou expirou
     */
    Visualizador validar(String token);
}
