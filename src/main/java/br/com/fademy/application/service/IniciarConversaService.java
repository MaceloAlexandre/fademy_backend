package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.ConversaGateway;
import br.com.fademy.application.gateway.ConversaGateway.Resultado;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.IniciarConversaUseCase;
import br.com.fademy.domain.conversa.Conversa;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;

public class IniciarConversaService implements IniciarConversaUseCase {

    private final UsuarioGateway usuarios;
    private final BloqueioGateway bloqueios;
    private final SeguirGateway seguires;
    private final ConversaGateway conversas;

    public IniciarConversaService(UsuarioGateway usuarios, BloqueioGateway bloqueios, SeguirGateway seguires,
                                  ConversaGateway conversas) {
        this.usuarios = usuarios;
        this.bloqueios = bloqueios;
        this.seguires = seguires;
        this.conversas = conversas;
    }

    @Override
    public Resultado executar(Visualizador visualizador, String outroId) {
        if (visualizador.id().equals(outroId)) {
            throw new DadosInvalidosException("Cannot message yourself");
        }
        Usuario eu = usuarios.doVisualizador(visualizador);
        Usuario outro = usuarios.buscarPorId(outroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("User not found"));
        if (!eu.mesmaInstituicao(outro) || bloqueios.existeEntre(eu.id(), outro.id())) {
            throw new AcessoNegadoException("Forbidden");
        }
        if (!seguires.seguemUmAoOutro(eu.id(), outro.id())) {
            throw new AcessoNegadoException("Both users must follow each other to start a chat");
        }
        return conversas.buscarOuCriar(Conversa.entre(eu, outro, Instant.now()));
    }
}
