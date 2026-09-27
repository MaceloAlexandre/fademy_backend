package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.EditarPerfilUseCase.EdicaoDePerfil;
import br.com.fademy.application.usecase.VerPerfilUseCase.Perfil;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.BeforeEach;
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

class PerfilServicesTest {

    private final UsuarioGateway usuarios = mock();
    private final SeguirGateway seguires = mock();
    private final BloqueioGateway bloqueios = mock();
    private final ArmazenamentoGateway armazenamento = mock();
    private final VerPerfilService ver = new VerPerfilService(usuarios, seguires, bloqueios);
    private final EditarPerfilService editar = new EditarPerfilService(usuarios, armazenamento);

    private final Usuario ana = usuario("ana", "inst", false);
    private final Visualizador visualizador = new Visualizador("ana", Papel.ALUNO, "inst");

    @BeforeEach
    void eu() {
        when(usuarios.doVisualizador(visualizador)).thenReturn(ana);
        when(seguires.contarSeguidores(any())).thenReturn(3L);
        when(seguires.contarSeguindo(any())).thenReturn(5L);
    }

    @Test
    void perfilPrivadoSemFollowAceitoNaoMostraNumeros() {
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "inst", true)));
        when(seguires.buscar("ana", "bia")).thenReturn(Optional.of(follow(StatusSeguir.PENDING)));

        Perfil perfil = ver.executar(visualizador, "bia");

        assertThat(perfil.statusDoVisualizador()).isEqualTo(StatusSeguir.PENDING);
        assertThat(perfil.seguidores()).isNull();
        assertThat(perfil.seguindo()).isNull();
    }

    @Test
    void perfilPrivadoComFollowAceitoMostraNumeros() {
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "inst", true)));
        when(seguires.buscar("ana", "bia")).thenReturn(Optional.of(follow(StatusSeguir.ACCEPTED)));

        Perfil perfil = ver.executar(visualizador, "bia");

        assertThat(perfil.seguidores()).isEqualTo(3L);
        assertThat(perfil.seguindo()).isEqualTo(5L);
    }

    @Test
    void quemMeBloqueouNaoExisteParaMim() {
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "inst", false)));
        when(bloqueios.bloqueou("bia", "ana")).thenReturn(true);

        assertThatThrownBy(() -> ver.executar(visualizador, "bia"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("User not found");
    }

    @Test
    void quemEuBloqueeiApareceMarcado() {
        when(usuarios.buscarPorId("bia")).thenReturn(Optional.of(usuario("bia", "inst", false)));
        when(bloqueios.bloqueou("ana", "bia")).thenReturn(true);

        assertThat(ver.executar(visualizador, "bia").bloqueadoPeloVisualizador()).isTrue();
    }

    @Test
    void validacoesDaEdicao() {
        erro(new EdicaoDePerfil(true, "true", false, null, null), "isPrivate must be a boolean");
        erro(new EdicaoDePerfil(true, null, false, null, null), "isPrivate must be a boolean");
        erro(new EdicaoDePerfil(false, null, true, "http://a", null), "avatarUrl and avatarKey must be strings");
        erro(new EdicaoDePerfil(false, null, true, 10, "chave"), "avatarUrl and avatarKey must be strings");
        erro(new EdicaoDePerfil(false, null, false, null, null), "No valid fields to update");
        verify(usuarios, never()).atualizar(any());
    }

    @Test
    void fotoQueNaoFoiEnviada() {
        when(armazenamento.existe("avatars/x.jpg")).thenReturn(false);

        erro(new EdicaoDePerfil(false, null, true, "http://a", "avatars/x.jpg"),
                "Profile photo not found in storage — please upload a photo first");
        verify(usuarios, never()).atualizar(any());
    }

    @Test
    void editaPrivacidadeEFoto() {
        when(armazenamento.existe("avatars/x.jpg")).thenReturn(true);

        Usuario editado = editar.executar(visualizador,
                new EdicaoDePerfil(true, true, true, "http://nova", "avatars/x.jpg"));

        assertThat(editado.privado()).isTrue();
        assertThat(editado.avatarUrl()).isEqualTo("http://nova");
        verify(usuarios).atualizar(ana);
    }

    private void erro(EdicaoDePerfil edicao, String mensagem) {
        assertThatThrownBy(() -> editar.executar(visualizador, edicao))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(mensagem);
    }

    private static Seguir follow(StatusSeguir status) {
        return new Seguir("f", "ana", "bia", status, Instant.now());
    }

    private static Usuario usuario(String id, String instituicao, boolean privado) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(
                id, instituicao, id, id + "@x.com", "hash", "http://avatar", privado, true, Instant.now()));
    }
}
