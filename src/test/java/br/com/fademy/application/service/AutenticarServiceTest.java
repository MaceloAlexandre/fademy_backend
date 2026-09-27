package br.com.fademy.application.service;

import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticarServiceTest {

    private final UsuarioGateway usuarios = mock();
    private final SenhaGateway senhas = mock();
    private final TokenGateway tokens = mock();
    private final AutenticarService service = new AutenticarService(usuarios, senhas, tokens);

    @Test
    void credenciaisCorretasDevolvemToken() {
        Usuario ana = usuario(true);
        when(usuarios.buscarPorEmail("ana@x.com")).thenReturn(Optional.of(ana));
        when(senhas.confere("segredo", "hash")).thenReturn(true);
        when(tokens.gerar(ana)).thenReturn("token");

        assertThat(service.executar("ana@x.com", "segredo")).isEqualTo("token");
    }

    @Test
    void emailInexistente() {
        when(usuarios.buscarPorEmail("ninguem@x.com")).thenReturn(Optional.empty());

        credenciaisInvalidas("ninguem@x.com", "segredo");
    }

    @Test
    void senhaErrada() {
        when(usuarios.buscarPorEmail("ana@x.com")).thenReturn(Optional.of(usuario(true)));
        when(senhas.confere("errada", "hash")).thenReturn(false);

        credenciaisInvalidas("ana@x.com", "errada");
    }

    @Test
    void usuarioInativo() {
        when(usuarios.buscarPorEmail("ana@x.com")).thenReturn(Optional.of(usuario(false)));
        when(senhas.confere("segredo", "hash")).thenReturn(true);

        credenciaisInvalidas("ana@x.com", "segredo");
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, segredo", "ana@x.com, NULL", "'', segredo", "ana@x.com, ''"}, nullValues = "NULL")
    void faltaEmailOuSenha(String email, String senha) {
        assertThatThrownBy(() -> service.executar(email, senha))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("email and password are required");
    }

    private void credenciaisInvalidas(String email, String senha) {
        assertThatThrownBy(() -> service.executar(email, senha))
                .isInstanceOf(NaoAutenticadoException.class)
                .hasMessage("Invalid credentials");
    }

    private static Usuario usuario(boolean ativo) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(
                "id", "inst", "Ana", "ana@x.com", "hash", null, false, ativo, Instant.now()));
    }
}
