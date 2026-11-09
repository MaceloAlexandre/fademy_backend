package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MensagensComFiltroDePalavrasTest {

    private final Visualizador ana = new Visualizador("ana", Papel.ALUNO, "inst");
    private final MensagensUseCase proximo = mock(MensagensUseCase.class);
    private final MensagensUseCase filtro = new MensagensComFiltroDePalavras(proximo);

    @Test
    void trocaPalavraProibidaPorAsteriscosDeMesmoTamanho() {
        filtro.enviar(ana, "c1", "que porcaria", List.of());

        verify(proximo).enviar(ana, "c1", "que ********", List.of());
    }

    @Test
    void ignoraMaiusculasEAceitaAcento() {
        filtro.enviar(ana, "c1", "Que PORCARIA, seu Otário", List.of());

        verify(proximo).enviar(ana, "c1", "Que ********, seu ******", List.of());
    }

    @Test
    void naoMexeNaPalavraQueSoContemAProibida() {
        filtro.enviar(ana, "c1", "fui na drogaria", List.of());

        verify(proximo).enviar(ana, "c1", "fui na drogaria", List.of());
    }

    @Test
    void mensagemSemTextoPassaDoJeitoQueVeio() {
        filtro.enviar(ana, "c1", null, List.of());

        verify(proximo).enviar(ana, "c1", null, List.of());
    }
}
