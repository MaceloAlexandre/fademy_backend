package br.com.fademy.application.usecase;

import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;

/**
 * Gera um convite (SPEC RN08, RN10).
 */
public interface GerarConviteUseCase {

    Convite executar(Visualizador visualizador, String papelAlvo, Integer maxUsos, Instant expiraEm);
}
