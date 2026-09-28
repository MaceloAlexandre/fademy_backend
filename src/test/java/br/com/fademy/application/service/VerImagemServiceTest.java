package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.midia.Imagem;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerImagemServiceTest {

    private final ArmazenamentoGateway armazenamento = mock(ArmazenamentoGateway.class);
    private final VerImagemService service = new VerImagemService(armazenamento);

    @Test
    void devolveAImagemGuardada() {
        Imagem imagem = new Imagem(new byte[]{9}, "image/png");
        when(armazenamento.ler("avatars/a.png")).thenReturn(Optional.of(imagem));

        assertThat(service.executar("avatars/a.png")).isSameAs(imagem);
    }

    @Test
    void chaveInexistenteEhNaoEncontrada() {
        when(armazenamento.ler("avatars/x.png")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executar("avatars/x.png"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Image not found");
    }
}
