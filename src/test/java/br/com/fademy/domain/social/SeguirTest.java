package br.com.fademy.domain.social;

import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SeguirTest {

    private final Usuario ana = usuario("ana", false);

    @Test
    void perfilPublicoEhSeguidoNaHora() {
        Seguir seguir = Seguir.solicitar(ana, usuario("bia", false), Instant.now());

        assertThat(seguir.status()).isEqualTo(StatusSeguir.ACCEPTED);
        assertThat(seguir.seguidorId()).isEqualTo("ana");
        assertThat(seguir.seguidoId()).isEqualTo("bia");
        assertThat(seguir.notificacaoParaOSeguido()).isEqualTo(TipoNotificacao.NEW_FOLLOWER);
    }

    @Test
    void perfilPrivadoRecebePedido() {
        Seguir seguir = Seguir.solicitar(ana, usuario("bia", true), Instant.now());

        assertThat(seguir.status()).isEqualTo(StatusSeguir.PENDING);
        assertThat(seguir.notificacaoParaOSeguido()).isEqualTo(TipoNotificacao.FOLLOW_REQUEST);
    }

    static Usuario usuario(String id, boolean privado) {
        return Papel.ALUNO.criarUsuario(new DadosUsuario(
                id, "inst", id, id + "@x.com", "hash", null, privado, true, Instant.now()));
    }
}
