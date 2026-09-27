package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.RevogarConviteUseCase;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public class RevogarConviteService implements RevogarConviteUseCase {

    private final UsuarioGateway usuarios;
    private final ConviteGateway convites;

    public RevogarConviteService(UsuarioGateway usuarios, ConviteGateway convites) {
        this.usuarios = usuarios;
        this.convites = convites;
    }

    @Override
    public void executar(Visualizador visualizador, String token) {
        Usuario usuario = usuarios.doVisualizador(visualizador);
        if (!usuario.podeGerarConvite()) {
            throw new AcessoNegadoException("Insufficient permissions");
        }
        Convite convite = convites.buscarPorToken(token)
                .filter(c -> c.daInstituicao(usuario.instituicaoId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Invite not found"));
        convite.revogar();
        convites.atualizarStatus(convite);
    }
}
