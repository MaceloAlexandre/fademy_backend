package br.com.fademy.domain.conversa;

import br.com.fademy.domain.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MensagemTest {

    @Test
    void textoSemEspacosNasPontas() {
        Mensagem m = Mensagem.escrever("c", "ana", "  oi  ", 0, Instant.now());

        assertThat(m.texto()).isEqualTo("oi");
        assertThat(m.midias()).isEmpty();
    }

    @Test
    void soImagemVale() {
        Mensagem m = Mensagem.escrever("c", "ana", "   ", 1, Instant.now()).comMidias(List.of("http://a"));

        assertThat(m.texto()).isNull();
        assertThat(m.midias()).containsExactly("http://a");
    }

    @Test
    void semTextoNemImagem() {
        assertThatThrownBy(() -> Mensagem.escrever("c", "ana", " ", 0, Instant.now()))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("text or media is required");
    }
}
