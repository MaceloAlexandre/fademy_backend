package br.com.fademy.application.service;

import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.VerPainelAdminUseCase;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public class VerPainelAdminService implements VerPainelAdminUseCase {

    private final UsuarioGateway usuarios;
    private final InstituicaoGateway instituicoes;

    public VerPainelAdminService(UsuarioGateway usuarios, InstituicaoGateway instituicoes) {
        this.usuarios = usuarios;
        this.instituicoes = instituicoes;
    }

    @Override
    public Painel executar(Visualizador visualizador) {
        Usuario eu = usuarios.doVisualizador(visualizador);
        if (!eu.podeAcessarPainelAdmin()) {
            throw new AcessoNegadoException("Insufficient permissions");
        }
        Instituicao instituicao = instituicoes.buscarPorId(eu.instituicaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Institution not found"));
        return new Painel(instituicao, usuarios.contarDaInstituicao(instituicao.id()));
    }
}
