package br.com.fademy.domain.notificacao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TipoNotificacaoTest {

    // Tabela da SPEC RN34.
    @ParameterizedTest
    @CsvSource({
            "NEW_FOLLOWER,     new_follower,     Follow",
            "FOLLOW_REQUEST,   follow_request,   Follow",
            "FOLLOW_ACCEPTED,  follow_accepted,  Follow",
            "POST_COMMENT,     post_comment,     Post",
            "INVITE_RECEIVED,  invite_received,  User",
            "MESSAGE_RECEIVED, message_received, Conversation",
    })
    void textoEReferenciaDeCadaTipo(TipoNotificacao tipo, String texto, String referencia) {
        assertThat(tipo.texto()).isEqualTo(texto);
        assertThat(tipo.modeloReferencia()).isEqualTo(referencia);
        assertThat(TipoNotificacao.deTexto(texto)).isEqualTo(tipo);
    }
}
