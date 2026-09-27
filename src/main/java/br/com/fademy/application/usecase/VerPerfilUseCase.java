package br.com.fademy.application.usecase;

import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

/**
 * Perfil de um usuário visto por quem pede (SPEC RN17).
 */
public interface VerPerfilUseCase {

    Perfil executar(Visualizador visualizador, String usuarioId);

    /**
     * @param statusDoVisualizador follow do visualizador para o usuário, ou null
     * @param seguidores           null quando o perfil privado não mostra os números
     * @param seguindo             idem
     */
    record Perfil(Usuario usuario, StatusSeguir statusDoVisualizador, boolean bloqueadoPeloVisualizador,
                  Long seguidores, Long seguindo) {
    }
}
