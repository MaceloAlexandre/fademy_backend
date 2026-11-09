package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MensagensComLimiteDeTamanhoTest {

    private final Visualizador ana = new Visualizador("ana", Papel.ALUNO, "inst");
    private final MensagensUseCase proximo = mock(MensagensUseCase.class);
    private final MensagensUseCase limite = new MensagensComLimiteDeTamanho(proximo);

    @Test
    void textoAcimaDoLimiteNemChegaNaProximaCamada() {
        assertThatThrownBy(() -> limite.enviar(ana, "c1", "a".repeat(501), List.of()))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("text is too long");

        verifyNoInteractions(proximo);
    }

    @Test
    void textoNoLimiteExatoPassa() {
        limite.enviar(ana, "c1", "a".repeat(500), List.of());

        verify(proximo).enviar(ana, "c1", "a".repeat(500), List.of());
    }

    @Test
    void mensagemSemTextoPassa() {
        limite.enviar(ana, "c1", null, List.of());

        verify(proximo).enviar(ana, "c1", null, List.of());
    }
}
