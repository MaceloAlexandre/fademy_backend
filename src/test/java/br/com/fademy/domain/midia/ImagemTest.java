package br.com.fademy.domain.midia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemTest {

    @ParameterizedTest
    @CsvSource({"image/jpeg, jpg", "image/png, png", "image/webp, webp"})
    void tiposAceitosGeramChaveComAExtensaoCerta(String tipo, String extensao) {
        String chave = new Imagem(new byte[]{1}, tipo).novaChave("avatars");

        assertThat(chave).matches("avatars/[0-9a-f-]{36}\\." + extensao);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "application/pdf", "text/plain"})
    void outrosTiposNaoSaoAceitos(String tipo) {
        assertThat(Imagem.aceita(tipo)).isFalse();
        assertThatThrownBy(() -> new Imagem(new byte[]{1}, tipo)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadaChaveEhNova() {
        Imagem imagem = new Imagem(new byte[]{1}, "image/png");

        assertThat(imagem.novaChave("posts")).isNotEqualTo(imagem.novaChave("posts"));
    }
}
