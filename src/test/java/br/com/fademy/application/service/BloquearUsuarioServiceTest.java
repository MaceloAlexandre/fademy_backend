package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BloquearUsuarioServiceTest {

    private final UsuarioGateway usuarios = mock();
    private final SeguirGateway seguires = mock();
    private final BloqueioGateway bloqueios = mock();
    private final NotificacaoGateway notificacoes = mock();
    private final TransacaoGateway transacao = mock();
    private final BloquearUsuarioService service =
            new BloquearUsuarioService(usuarios, seguires, bloqueios, notificacoes, transacao);

    private final Visualizador visualizador = new Visualizador("ana", Papel.ALUNO, "inst");

    @Test
    void postgresNaTransacaoEMongoDepois() {
        when(usuarios.doVisualizador(visualizador)).thenReturn(usuario("ana", "inst"));
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "inst")));
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transacao).executar(any(Runnable.class));

        service.executar(visualizador, "bia");

        InOrder ordem = inOrder(transacao, seguires, bloqueios, notificacoes);
        ordem.verify(transacao).executar(any(Runnable.class));
        ordem.verify(seguires).removerEntre("ana", "bia");
        ordem.verify(bloqueios).bloquear("ana", "bia");
        ordem.verify(notificacoes).removerPedidosDeSeguirEntre("ana", "bia");
    }

    @Test
    void bloquearASiMesmo() {
        assertThatThrownBy(() -> service.executar(visualizador, "ana"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Cannot block yourself");
        verifyNoInteractions(usuarios, transacao, notificacoes);
    }

    @Test
    void alvoDeOutraInstituicao() {
        when(usuarios.doVisualizador(visualizador)).thenReturn(usuario("ana", "inst"));
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "outra")));

        assertThatThrownBy(() -> service.executar(visualizador, "bia"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("User not found");
        verifyNoInteractions(transacao, notificacoes);
    }

    private static Usuario usuario(String id, String instituicao) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(
                id, instituicao, id, id + "@x.com", "hash", null, false, true, Instant.now()));
    }
}
