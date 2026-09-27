package br.com.fademy.application.service;

import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.ListarNotificacoesUseCase.NotificacaoDetalhada;
import br.com.fademy.application.usecase.ListarNotificacoesUseCase.Resultado;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarNotificacoesServiceTest {

    private final NotificacaoGateway notificacoes = mock();
    private final UsuarioGateway usuarios = mock();
    private final SeguirGateway seguires = mock();
    private final ListarNotificacoesService service = new ListarNotificacoesService(notificacoes, usuarios, seguires);

    @Test
    void juntaAtoresEStatusDosPedidosDoPostgres() {
        Visualizador eu = new Visualizador("eu", Papel.ALUNO, "inst");
        when(notificacoes.listar("eu", null, 21)).thenReturn(List.of(
                notificacao("n3", TipoNotificacao.FOLLOW_REQUEST, "ana", "follow-ana"),
                notificacao("n2", TipoNotificacao.FOLLOW_REQUEST, "bia", "follow-apagado"),
                notificacao("n1", TipoNotificacao.NEW_FOLLOWER, "ana", "follow-x")));
        when(usuarios.buscarPorIds(any())).thenReturn(List.of(usuario("ana"), usuario("bia")));
        when(seguires.buscarPorIds(any())).thenReturn(List.of(
                new Seguir("follow-ana", "ana", "eu", StatusSeguir.ACCEPTED, Instant.now())));
        when(notificacoes.contarNaoLidas("eu")).thenReturn(7L);

        Resultado resultado = service.executar(eu, null, null);

        List<NotificacaoDetalhada> itens = resultado.pagina().itens();
        assertThat(itens).extracting(d -> d.ator().id()).containsExactly("ana", "bia", "ana");
        assertThat(itens).extracting(NotificacaoDetalhada::statusDoPedido)
                .containsExactly(StatusSeguir.ACCEPTED, null, null);
        assertThat(resultado.naoLidas()).isEqualTo(7L);
        assertThat(resultado.pagina().temMais()).isFalse();
    }

    private static Notificacao notificacao(String id, TipoNotificacao tipo, String ator, String ref) {
        return new Notificacao(id, "eu", ator, tipo, ref, false, Instant.now());
    }

    private static Usuario usuario(String id) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(id, "inst", id, id + "@x.com", "h", "http://" + id,
                false, true, Instant.now()));
    }
}
