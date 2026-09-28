package br.com.fademy.domain.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    static Usuario usuario(Papel papel) {
        return papel.criarUsuario(new DadosUsuario(
                "id", "instituicao", "Ana", "ana@x.com", "hash", "https://avatar", false, true, Instant.now()));
    }

    @ParameterizedTest
    @CsvSource({
            "ADMIN,        true,    true,  true",
            "COORDENADOR,  true,    true,  false",
            "PROFESSOR,    true,    true,  false",
            "FUNCIONARIO,  true,    true,  false",
            "ALUNO,        false,   false, false",
    })
    void permissoesPorPapel(Papel papel, boolean convida, boolean link, boolean painel) {
        Usuario usuario = usuario(papel);

        assertThat(usuario.podeGerarConvite()).isEqualTo(convida);
        assertThat(usuario.podePublicarLink()).isEqualTo(link);
        assertThat(usuario.podeAcessarPainelAdmin()).isEqualTo(painel);
    }

    @ParameterizedTest
    @EnumSource(Papel.class)
    void soAdminConvidaAdmin(Papel papel) {
        Usuario usuario = usuario(papel);

        for (Papel alvo : Papel.values()) {
            boolean esperado = usuario.podeGerarConvite() && (alvo != Papel.ADMIN || papel == Papel.ADMIN);
            assertThat(usuario.podeGerarConvitePara(alvo))
                    .as("%s convidando %s", papel, alvo)
                    .isEqualTo(esperado);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "false,     outro,        false,        true",
            "true,      outro,        false,        false",
            "true,      outro,        true,         true",
            "true,      id,           false,        true",
    })
    void perfilPrivadoSoMostraConteudoAoDonoEAQuemSegue(boolean privado, String visualizador, boolean segue,
                                                         boolean mostra) {
        Usuario usuario = Papel.ALUNO.criarUsuario(new DadosUsuario(
                "id", "inst", "Ana", "ana@x.com", "hash", null, privado, true, Instant.now()));

        assertThat(usuario.mostraConteudoPara(visualizador, segue)).isEqualTo(mostra);
    }

    @Test
    void editarPrivacidadeEAvatar() {
        Usuario usuario = usuario(Papel.ALUNO);

        usuario.definirPrivacidade(true);
        usuario.trocarAvatar("avatars/nova.png");

        assertThat(usuario.privado()).isTrue();
        assertThat(usuario.avatarChave()).isEqualTo("avatars/nova.png");
    }
}
