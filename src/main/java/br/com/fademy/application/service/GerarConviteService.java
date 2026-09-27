package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.GerarConviteUseCase;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;

public class GerarConviteService implements GerarConviteUseCase {

    private final UsuarioGateway usuarios;
    private final ConviteGateway convites;

    public GerarConviteService(UsuarioGateway usuarios, ConviteGateway convites) {
        this.usuarios = usuarios;
        this.convites = convites;
    }

    @Override
    public Convite executar(Visualizador visualizador, String papelAlvo, Integer maxUsos, Instant expiraEm) {
        Usuario autor = usuarios.doVisualizador(visualizador);
        if (!autor.podeGerarConvite()) {
            throw new AcessoNegadoException("Insufficient permissions");
        }
        return convites.criar(Convite.gerar(autor, papelAlvo, maxUsos, expiraEm, Instant.now()));
    }
}
