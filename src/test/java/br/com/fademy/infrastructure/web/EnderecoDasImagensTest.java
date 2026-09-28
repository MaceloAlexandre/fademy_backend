package br.com.fademy.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnderecoDasImagensTest {

    private final EnderecoDasImagens imagens = new EnderecoDasImagens("http://192.168.0.10:8333/fademy");

    @Test
    void montaAUrlComOEnderecoAtual() {
        assertThat(imagens.url("avatars/a.png")).isEqualTo("http://192.168.0.10:8333/fademy/avatars/a.png");
        assertThat(imagens.urls(List.of("posts/1.jpg", "posts/2.jpg")))
                .containsExactly("http://192.168.0.10:8333/fademy/posts/1.jpg", "http://192.168.0.10:8333/fademy/posts/2.jpg");
    }

    @Test
    void semImagemSemUrl() {
        assertThat(imagens.url(null)).isNull();
    }
}
