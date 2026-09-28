package br.com.fademy.application.usecase;

import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public interface VerPerfilUseCase {

    Perfil executar(Visualizador visualizador, String usuarioId);

    record Perfil(Usuario usuario, StatusSeguir statusDoVisualizador, boolean bloqueadoPeloVisualizador,
                  Long seguidores, Long seguindo) {
    }
}
