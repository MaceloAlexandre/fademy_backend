package br.com.fademy.application.usecase;

import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Visualizador;

public interface VerPainelAdminUseCase {

    Painel executar(Visualizador visualizador);

    record Painel(Instituicao instituicao, long usuarios) {
    }
}
