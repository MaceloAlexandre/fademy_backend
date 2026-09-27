package br.com.fademy.application.usecase;

import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Visualizador;

/**
 * Painel administrativo da instituição de quem pede (SPEC RN44).
 */
public interface VerPainelAdminUseCase {

    Painel executar(Visualizador visualizador);

    record Painel(Instituicao instituicao, long usuarios) {
    }
}
