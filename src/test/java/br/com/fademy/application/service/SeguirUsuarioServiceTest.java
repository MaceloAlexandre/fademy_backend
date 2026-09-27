package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.ConflitoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SeguirUsuarioServiceTest {

    private final UsuarioGateway usuarios = mock();
    private final SeguirGateway seguires = mock();
    private final BloqueioGateway bloqueios = mock();
    private final NotificacaoGateway notificacoes = mock();
    private final SeguirUsuarioService service = new SeguirUsuarioService(usuarios, seguires, bloqueios, notificacoes);

    private final Usuario ana = usuario("ana", "inst", false);
    private final Visualizador visualizador = new Visualizador("ana", Papel.ALUNO, "inst");

    @BeforeEach
    void cenario() {
        when(usuarios.doVisualizador(visualizador)).thenReturn(ana);
        when(seguires.criar(any())).thenAnswer(i -> {
            Seguir s = i.getArgument(0);
            return new Seguir("id-follow", s.seguidorId(), s.seguidoId(), s.status(), s.criadoEm());
        });
    }

    @Test
    void seguirPerfilPublicoNotificaNovoSeguidor() {
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "inst", false)));

        Seguir seguir = service.executar(visualizador, "bia");

        assertThat(seguir.status()).isEqualTo(StatusSeguir.ACCEPTED);
        ArgumentCaptor<Notificacao> n = ArgumentCaptor.captor();
        verify(notificacoes).criar(n.capture());
        assertThat(n.getValue().tipo()).isEqualTo(TipoNotificacao.NEW_FOLLOWER);
        assertThat(n.getValue().destinatarioId()).isEqualTo("bia");
        assertThat(n.getValue().atorId()).isEqualTo("ana");
        assertThat(n.getValue().refId()).isEqualTo("id-follow");
        verify(notificacoes, never()).removerPedidoDeSeguir(any(), any());
    }

    @Test
    void seguirPerfilPrivadoApagaPedidoAntigoAntesDeCriarONovo() {
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "inst", true)));

        service.executar(visualizador, "bia");

        InOrder ordem = inOrder(notificacoes);
        ordem.verify(notificacoes).removerPedidoDeSeguir("ana", "bia");
        ordem.verify(notificacoes).criar(any());
    }

    @Test
    void seguirASiMesmoNemConsultaNada() {
        assertThatThrownBy(() -> service.executar(visualizador, "ana"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Cannot follow yourself");
        verifyNoInteractions(bloqueios, seguires, notificacoes);
    }

    @Test
    void bloqueioVemAntesDeProcurarOAlvo() {
        when(bloqueios.existeEntre("ana", "bia")).thenReturn(true);

        assertThatThrownBy(() -> service.executar(visualizador, "bia"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Forbidden");
        verify(usuarios, never()).buscarPorId(any());
    }

    @Test
    void alvoDeOutraInstituicaoNaoExiste() {
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "outra", false)));

        assertThatThrownBy(() -> service.executar(visualizador, "bia"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("User not found");
    }

    @Test
    void jaSegue() {
        Usuario bia = usuario("bia", "inst", false);
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(bia));
        when(seguires.buscar("ana", "bia"))
                .thenReturn(Optional.of(new Seguir("f", "ana", "bia", StatusSeguir.PENDING, Instant.now())));

        assertThatThrownBy(() -> service.executar(visualizador, "bia"))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Already following or request pending");
        verify(seguires, never()).criar(any());
    }

    private static Usuario usuario(String id, String instituicao, boolean privado) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(
                id, instituicao, id, id + "@x.com", "hash", null, privado, true, Instant.now()));
    }
}
