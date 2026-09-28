package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase.Cadastro;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.convite.StatusConvite;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.usuario.Aluno;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrarViaConviteServiceTest {

    private final ConviteGateway convites = mock();
    private final UsuarioGateway usuarios = mock();
    private final ArmazenamentoGateway armazenamento = mock();
    private final SenhaGateway senhas = mock();
    private final TransacaoGateway transacao = mock();
    private final NotificacaoGateway notificacoes = mock();
    private final RegistrarViaConviteService service = new RegistrarViaConviteService(
            convites, usuarios, armazenamento, senhas, transacao, notificacoes);

    private final Convite convite = new Convite("id-convite", "inst", "id-professor", Papel.ALUNO, "tok", 10, 0,
            StatusConvite.ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
    private final Cadastro cadastro = new Cadastro("tok", "Ana", "ana@x.com", "segredo", "http://a", "avatars/a.jpg");

    @BeforeEach
    void cenarioFeliz() {
        when(convites.buscarPorToken("tok")).thenReturn(Optional.of(convite));
        when(armazenamento.existe("avatars/a.jpg")).thenReturn(true);
        when(senhas.gerarHash("segredo")).thenReturn("hash");
        when(transacao.executar(any(Supplier.class))).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        when(usuarios.criar(any())).thenAnswer(i -> comId(i.getArgument(0)));
        when(convites.consumirVaga("id-convite")).thenReturn(true);
    }

    @Test
    void criaAlunoNaInstituicaoDoConviteConsomeVagaENotificaDepois() {
        Usuario criado = service.executar(cadastro);

        assertThat(criado).isInstanceOf(Aluno.class);
        assertThat(criado.instituicaoId()).isEqualTo("inst");
        assertThat(criado.senhaHash()).isEqualTo("hash");

        InOrder ordem = inOrder(transacao, usuarios, convites, notificacoes);
        ordem.verify(transacao).executar(any(Supplier.class));
        ordem.verify(usuarios).criar(any());
        ordem.verify(convites).consumirVaga("id-convite");
        ArgumentCaptor<Notificacao> notificacao = ArgumentCaptor.captor();
        ordem.verify(notificacoes).criar(notificacao.capture());

        assertThat(notificacao.getValue().tipo()).isEqualTo(TipoNotificacao.INVITE_RECEIVED);
        assertThat(notificacao.getValue().destinatarioId()).isEqualTo("id-professor");
        assertThat(notificacao.getValue().atorId()).isEqualTo("id-novo");
        assertThat(notificacao.getValue().refId()).isEqualTo("id-novo");
        assertThat(notificacao.getValue().lida()).isFalse();
    }

    @Test
    void campoFaltando() {
        erro(new Cadastro("tok", "Ana", "ana@x.com", "segredo", "http://a", null),
                "token, name, email, password, avatarUrl, and avatarKey are required");
        verifyNoInteractions(convites);
    }

    @Test
    void conviteInexistente() {
        when(convites.buscarPorToken("tok")).thenReturn(Optional.empty());

        erro(cadastro, "Invalid invite token");
    }

    @Test
    void conviteInvalidoParaAntesDeOlharAFoto() {
        convite.revogar();

        erro(cadastro, "Invite is no longer valid");
        verifyNoInteractions(armazenamento);
    }

    @Test
    void fotoQueNaoExiste() {
        when(armazenamento.existe("avatars/a.jpg")).thenReturn(false);

        erro(cadastro, "Profile photo not found in storage — please upload a photo first");
        verify(usuarios, never()).criar(any());
    }

    @Test
    void semVagaNaHoraDeGravarNaoNotifica() {
        when(convites.consumirVaga("id-convite")).thenReturn(false);

        erro(cadastro, "Invite has no remaining slots");
        verifyNoInteractions(notificacoes);
    }

    private void erro(Cadastro c, String mensagem) {
        assertThatThrownBy(() -> service.executar(c))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(mensagem);
    }

    private static Usuario comId(Usuario u) {
        return u.papel().criarUsuario(new DadosUsuario("id-novo", u.instituicaoId(), u.nome(), u.email(),
                u.senhaHash(), u.avatarChave(), u.privado(), u.ativo(), u.criadoEm()));
    }
}
