package br.com.fademy.application.gateway;

import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public interface TokenGateway {

    String gerar(Usuario usuario);

    Visualizador validar(String token);
}
