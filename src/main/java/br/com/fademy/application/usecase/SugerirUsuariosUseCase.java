package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public interface SugerirUsuariosUseCase {

    List<Usuario> executar(Visualizador visualizador, String limite);
}
