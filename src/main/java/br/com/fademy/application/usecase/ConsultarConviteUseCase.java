package br.com.fademy.application.usecase;

import br.com.fademy.domain.convite.SituacaoConvite;
import br.com.fademy.domain.usuario.Papel;

public interface ConsultarConviteUseCase {

    ConsultaConvite executar(String token);

    record ConsultaConvite(Papel papelAlvo, String nomeInstituicao, int vagasRestantes, SituacaoConvite situacao) {
    }
}
