package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.usecase.EnviarAvatarUseCase.ImagemEnviada;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EnviarAvatarServiceTest {

    private final ArmazenamentoGateway armazenamento = mock();
    private final EnviarAvatarService service = new EnviarAvatarService(armazenamento);

    @Test
    void enviaNaPastaDeAvatares() {
        when(armazenamento.enviar(startsWith("avatars/"), any())).thenReturn("http://s3/fademy/avatars/x.png");

        ImagemEnviada enviada = service.executar(new byte[]{1, 2}, "image/png");

        assertThat(enviada.chave()).startsWith("avatars/").endsWith(".png");
        assertThat(enviada.url()).isEqualTo("http://s3/fademy/avatars/x.png");
    }

    @Test
    void semArquivo() {
        assertThatThrownBy(() -> service.executar(null, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("file is required");
        verifyNoInteractions(armazenamento);
    }

    @Test
    void tipoNaoAceitoNaoGravaNada() {
        assertThatThrownBy(() -> service.executar(new byte[]{1}, "image/gif"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("file must be image/jpeg, image/png, or image/webp");
        verifyNoInteractions(armazenamento);
    }
}
