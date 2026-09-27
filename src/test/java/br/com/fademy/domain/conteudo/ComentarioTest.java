package br.com.fademy.domain.conteudo;

import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComentarioTest {

    private final Usuario ana = usuario("ana");
    private final Usuario bia = usuario("bia");

    @Test
    void escreverNaInstituicaoDoAutor() {
        Comentario c = Comentario.escrever(ana, "post", "  boa aula  ", Instant.now());

        assertThat(c.texto()).isEqualTo("boa aula");
        assertThat(c.autorId()).isEqualTo("ana");
        assertThat(c.instituicaoId()).isEqualTo("inst");
        assertThat(c.postId()).isEqualTo("post");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void textoObrigatorio(String texto) {
        assertThatThrownBy(() -> Comentario.escrever(ana, "post", texto, Instant.now()))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("text is required");
    }

    @Test
    void autorSoEhAvisadoDoComentarioDosOutros() {
        Post doAna = Post.publicar(ana, "post", null, List.of(), Instant.now());

        assertThat(doAna.notificaAutorSobreComentarioDe(bia)).isTrue();
        assertThat(doAna.notificaAutorSobreComentarioDe(ana)).isFalse();
    }

    private static Usuario usuario(String id) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(id, "inst", id, id + "@x.com", "h", null, false, true,
                Instant.now()));
    }
}
