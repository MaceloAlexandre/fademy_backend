package br.com.fademy.domain.notificacao;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacaoTest {

    @Test
    void novaNasceNaoLida() {
        Notificacao n = Notificacao.nova(TipoNotificacao.POST_COMMENT, "autor", "comentarista", "post", Instant.now());

        assertThat(n.lida()).isFalse();
        assertThat(n.id()).isNull();
    }

    @Test
    void soODestinatarioMexe() {
        Notificacao n = Notificacao.nova(TipoNotificacao.NEW_FOLLOWER, "bia", "ana", "f", Instant.now());

        n.garantirDestinatario("bia");
        assertThatThrownBy(() -> n.garantirDestinatario("ana"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Forbidden");
    }
}
