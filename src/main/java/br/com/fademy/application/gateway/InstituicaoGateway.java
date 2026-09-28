package br.com.fademy.application.gateway;

import br.com.fademy.domain.instituicao.Instituicao;

import java.util.Optional;

public interface InstituicaoGateway {

    Instituicao garantir(Instituicao instituicao);

    Optional<Instituicao> buscarPorId(String id);
}
