package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

/**
 * Busca de usuários por nome (SPEC RN19). Página e limite chegam como o cliente mandou.
 */
public interface BuscarUsuariosUseCase {

    Resultado executar(Visualizador visualizador, String trecho, String pagina, String limite);

    record Resultado(List<Usuario> usuarios, long total, int pagina, int limite) {
    }
}
