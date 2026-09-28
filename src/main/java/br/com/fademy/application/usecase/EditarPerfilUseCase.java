package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public interface EditarPerfilUseCase {

    Usuario executar(Visualizador visualizador, EdicaoDePerfil edicao);

    record EdicaoDePerfil(boolean alteraPrivacidade, Object privado,
                          boolean alteraAvatar, Object avatarUrl, Object avatarChave) {
    }
}
