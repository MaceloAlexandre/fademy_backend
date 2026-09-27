package br.com.fademy.application.gateway;

import br.com.fademy.domain.instituicao.Instituicao;

public interface InstituicaoGateway {

    /** Cria a instituição ou, se o slug já existe, atualiza o nome e a reativa. Devolve com id. */
    Instituicao garantir(Instituicao instituicao);
}
