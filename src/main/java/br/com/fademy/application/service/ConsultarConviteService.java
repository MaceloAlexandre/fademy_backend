package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.usecase.ConsultarConviteUseCase;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.instituicao.Instituicao;

import java.time.Instant;

public class ConsultarConviteService implements ConsultarConviteUseCase {

    private final ConviteGateway convites;
    private final InstituicaoGateway instituicoes;

    public ConsultarConviteService(ConviteGateway convites, InstituicaoGateway instituicoes) {
        this.convites = convites;
        this.instituicoes = instituicoes;
    }

    @Override
    public ConsultaConvite executar(String token) {
        Convite convite = convites.buscarPorToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Invite not found"));
        String nomeInstituicao = instituicoes.buscarPorId(convite.instituicaoId()).map(Instituicao::nome).orElse(null);
        return new ConsultaConvite(convite.papelAlvo(), nomeInstituicao, convite.vagasRestantes(),
                convite.situacao(Instant.now()));
    }
}
