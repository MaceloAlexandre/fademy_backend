package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.convite.StatusConvite;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConviteServicesTest {

    private final UsuarioGateway usuarios = mock();
    private final ConviteGateway convites = mock();

    private final Usuario aluno = usuario(Papel.ALUNO);
    private final Usuario professor = usuario(Papel.PROFESSOR);

    @Test
    void alunoNaoGeraConviteNemComCorpoInvalido() {
        when(usuarios.doVisualizador(any())).thenReturn(aluno);

        assertThatThrownBy(() -> new GerarConviteService(usuarios, convites)
                .executar(visualizador(aluno), null, null, null))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Insufficient permissions");
        verify(convites, never()).criar(any());
    }

    @Test
    void professorGeraConvite() {
        when(usuarios.doVisualizador(any())).thenReturn(professor);
        when(convites.criar(any())).thenAnswer(c -> c.getArgument(0));

        Convite convite = new GerarConviteService(usuarios, convites)
                .executar(visualizador(professor), "aluno", 30, null);

        assertThat(convite.convidadoPorId()).isEqualTo(professor.id());
        verify(convites).criar(convite);
    }

    @Test
    void alunoNaoRevoga() {
        when(usuarios.doVisualizador(any())).thenReturn(aluno);

        assertThatThrownBy(() -> new RevogarConviteService(usuarios, convites).executar(visualizador(aluno), "t"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Insufficient permissions");
    }

    @Test
    void conviteDeOutraInstituicaoNaoExiste() {
        when(usuarios.doVisualizador(any())).thenReturn(professor);
        when(convites.buscarPorToken("t")).thenReturn(Optional.of(convite("outra-inst")));

        assertThatThrownBy(() -> new RevogarConviteService(usuarios, convites).executar(visualizador(professor), "t"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Invite not found");
    }

    @Test
    void revogaConviteDaInstituicao() {
        Convite convite = convite("inst");
        when(usuarios.doVisualizador(any())).thenReturn(professor);
        when(convites.buscarPorToken("t")).thenReturn(Optional.of(convite));

        new RevogarConviteService(usuarios, convites).executar(visualizador(professor), "t");

        assertThat(convite.status()).isEqualTo(StatusConvite.REVOKED);
        verify(convites).atualizarStatus(convite);
    }

    private static Convite convite(String instituicao) {
        return new Convite("c", instituicao, "autor", Papel.ALUNO, "t", 10, 0, StatusConvite.ACTIVE,
                Instant.now().plusSeconds(3600), Instant.now());
    }

    private static Visualizador visualizador(Usuario u) {
        return new Visualizador(u.id(), u.papel(), u.instituicaoId());
    }

    private static Usuario usuario(Papel papel) {
        return papel.criarUsuario(new DadosUsuario(
                "id-" + papel.texto(), "inst", "Nome", "x@x.com", "hash", null, false, true, Instant.now()));
    }
}
