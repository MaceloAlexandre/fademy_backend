package br.com.fademy.domain.conversa;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversaTest {

    private final Conversa conversa = Conversa.entre(usuario("ana"), usuario("bia"), Instant.now());

    @Test
    void conversaEntreDois() {
        assertThat(conversa.membros()).containsExactly("ana", "bia");
        assertThat(conversa.criadaPorId()).isEqualTo("ana");
        assertThat(conversa.outroMembro("ana")).isEqualTo("bia");
        assertThat(conversa.outroMembro("bia")).isEqualTo("ana");
    }

    @Test
    void soMembroEntra() {
        conversa.garantirMembro("bia");
        assertThatThrownBy(() -> conversa.garantirMembro("carla"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Forbidden");
    }

    private static Usuario usuario(String id) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(id, "inst", id, id + "@x.com", "h", null, false, true,
                Instant.now()));
    }
}
