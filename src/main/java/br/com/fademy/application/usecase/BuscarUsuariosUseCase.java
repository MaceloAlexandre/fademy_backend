package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public interface BuscarUsuariosUseCase {

    Resultado executar(Visualizador visualizador, String trecho, String pagina, String limite);

    record Resultado(List<Usuario> usuarios, long total, int pagina, int limite) {
    }
}
