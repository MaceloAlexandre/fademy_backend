package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.SeguirUsuarioUseCase;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.ConflitoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;

public class SeguirUsuarioService implements SeguirUsuarioUseCase {

    private final UsuarioGateway usuarios;
    private final SeguirGateway seguires;
    private final BloqueioGateway bloqueios;
    private final NotificacaoGateway notificacoes;

    public SeguirUsuarioService(UsuarioGateway usuarios, SeguirGateway seguires, BloqueioGateway bloqueios,
                                NotificacaoGateway notificacoes) {
        this.usuarios = usuarios;
        this.seguires = seguires;
        this.bloqueios = bloqueios;
        this.notificacoes = notificacoes;
    }

    @Override
    public Seguir executar(Visualizador visualizador, String alvoId) {
        if (visualizador.id().equals(alvoId)) {
            throw new DadosInvalidosException("Cannot follow yourself");
        }
        if (bloqueios.existeEntre(visualizador.id(), alvoId)) {
            throw new AcessoNegadoException("Forbidden");
        }
        Usuario seguidor = usuarios.doVisualizador(visualizador);
        Usuario alvo = usuarios.buscarPorId(alvoId)
                .filter(seguidor::mesmaInstituicao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("User not found"));
        if (seguires.buscar(seguidor.id(), alvo.id()).isPresent()) {
            throw new ConflitoException("Already following or request pending");
        }

        Instant agora = Instant.now();
        Seguir seguir = seguires.criar(Seguir.solicitar(seguidor, alvo, agora));

        if (seguir.status() == StatusSeguir.PENDING) {
            notificacoes.removerPedidoDeSeguir(seguidor.id(), alvo.id());
        }
        notificacoes.criar(Notificacao.nova(
                seguir.notificacaoParaOSeguido(), alvo.id(), seguidor.id(), seguir.id(), agora));
        return seguir;
    }
}
