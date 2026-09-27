package br.com.fademy.domain.conteudo;

import br.com.fademy.domain.excecao.AcessoNegadoException;
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

class PostTest {

    private final Usuario aluno = usuario(Papel.ALUNO);
    private final Usuario professor = usuario(Papel.PROFESSOR);

    @Test
    void publicarNaInstituicaoDoAutor() {
        Post post = Post.publicar(aluno, "  Olá turma  ", null, List.of(" c1 ", "c2", "", "c1"), Instant.now());

        assertThat(post.texto()).isEqualTo("Olá turma");
        assertThat(post.autorId()).isEqualTo("id-aluno");
        assertThat(post.instituicaoId()).isEqualTo("inst");
        assertThat(post.categoriaIds()).containsExactly("c1", "c2");
        assertThat(post.temLink()).isFalse();
        assertThat(post.comentarios()).isZero();
        assertThat(post.imagens()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void textoObrigatorio(String texto) {
        assertThatThrownBy(() -> Post.publicar(aluno, texto, null, List.of(), Instant.now()))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("text is required");
    }

    @Test
    void alunoNaoPublicaLink() {
        assertThatThrownBy(() -> Post.publicar(aluno, "texto", "http://x", List.of(), Instant.now()))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Insufficient permissions");
        assertThat(Post.publicar(aluno, "texto", "   ", List.of(), Instant.now()).temLink()).isFalse();
    }

    @Test
    void professorPublicaLink() {
        Post post = Post.publicar(professor, "texto", " http://x ", null, Instant.now());

        assertThat(post.temLink()).isTrue();
        assertThat(post.linkUrl()).isEqualTo("http://x");
        assertThat(post.categoriaIds()).isEmpty();
    }

    @Test
    void anexarImagens() {
        Post post = Post.publicar(aluno, "texto", null, List.of(), Instant.now());

        post.anexarImagem("http://a");
        post.anexarImagem("http://b");

        assertThat(post.imagens()).containsExactly("http://a", "http://b");
    }

    private static Usuario usuario(Papel papel) {
        return papel.criarUsuario(new DadosUsuario("id-" + papel.texto(), "inst", "Nome", "x@x.com", "h", null,
                false, true, Instant.now()));
    }
}
