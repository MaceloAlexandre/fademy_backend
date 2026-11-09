package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MensagensDecoratorTest {

    @Test
    void todasAsCamadasDevolvemOListarDoServicoSemMudar() {
        Visualizador ana = new Visualizador("ana", Papel.ALUNO, "inst");
        Pagina<Mensagem> pagina = new Pagina<>(List.of(), false, null);
        MensagensUseCase servico = mock(MensagensUseCase.class);
        when(servico.listar(ana, "c1", null, "20")).thenReturn(pagina);

        MensagensUseCase camadas = new MensagensComAuditoria(
                new MensagensComLimiteDeTamanho(new MensagensComFiltroDePalavras(servico)), linha -> { });

        assertThat(camadas.listar(ana, "c1", null, "20")).isSameAs(pagina);
    }
}
