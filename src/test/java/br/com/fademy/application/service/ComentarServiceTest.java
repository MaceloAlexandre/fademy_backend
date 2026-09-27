package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.ComentarioGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.conteudo.Comentario;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ComentarServiceTest {

    private final UsuarioGateway usuarios = mock();
    private final PostGateway posts = mock();
    private final BloqueioGateway bloqueios = mock();
    private final ComentarioGateway comentarios = mock();
    private final NotificacaoGateway notificacoes = mock();
    private final TransacaoGateway transacao = mock();
    private final ComentarService service =
            new ComentarService(usuarios, posts, bloqueios, comentarios, notificacoes, transacao);

    private final Visualizador visualizador = new Visualizador("bia", Papel.ALUNO, "inst");
    private final Usuario bia = usuario("bia");

    @BeforeEach
    void cenario() {
        when(usuarios.doVisualizador(visualizador)).thenReturn(bia);
        when(posts.buscarPorId("p")).thenReturn(Optional.of(
                new Post("p", "ana", "inst", "texto", List.of(), null, List.of(), 0, Instant.now())));
        when(transacao.executar(any(Supplier.class))).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        when(comentarios.criar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void comentarioEContadorNaTransacaoENotificacaoDepois() {
        service.comentar(visualizador, "p", "boa");

        InOrder ordem = inOrder(transacao, comentarios, posts, notificacoes);
        ordem.verify(transacao).executar(any(Supplier.class));
        ordem.verify(comentarios).criar(any(Comentario.class));
        ordem.verify(posts).somarComentario("p");
        ordem.verify(notificacoes).criar(any());
    }

    @Test
    void textoInvalidoAntesDeProcurarOPost() {
        assertThatThrownBy(() -> service.comentar(visualizador, "p", " "))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("text is required");
        verifyNoInteractions(comentarios, transacao, notificacoes);
    }

    private static Usuario usuario(String id) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(id, "inst", id, id + "@x.com", "h", null, false, true,
                Instant.now()));
    }
}
