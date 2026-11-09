package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MensagensComAuditoriaTest {

    private final Visualizador ana = new Visualizador("ana", Papel.ALUNO, "inst");
    private final MensagensUseCase proximo = mock(MensagensUseCase.class);
    private final List<String> linhas = new ArrayList<>();
    private final MensagensUseCase auditoria = new MensagensComAuditoria(proximo, linhas::add);

    @Test
    void registraEnvioAceitoSemOTextoEDevolveAMensagem() {
        Mensagem gravada = new Mensagem("m1", "c1", "ana", "segredo", List.of(), Instant.now());
        when(proximo.enviar(ana, "c1", "segredo", List.of())).thenReturn(gravada);

        Mensagem devolvida = auditoria.enviar(ana, "c1", "segredo", List.of());

        assertThat(devolvida).isSameAs(gravada);
        assertThat(linhas).singleElement().asString()
                .startsWith("[auditoria] remetente=ana conversa=c1 resultado=ok tempo=")
                .endsWith("ms")
                .doesNotContain("segredo");
    }

    @Test
    void quebraDeLinhaNoIdDaConversaNaoCriaLinhaFalsaNaAuditoria() {
        auditoria.enviar(ana, "c1\r\n[auditoria] falsa", "x", List.of());

        assertThat(linhas).singleElement().asString()
                .doesNotContain("\n").doesNotContain("\r")
                .contains("conversa=c1__[auditoria] falsa");
    }

    @Test
    void registraEnvioRecusadoEDeixaOErroSeguir() {
        when(proximo.enviar(ana, "c1", "x", List.of())).thenThrow(new DadosInvalidosException("text is too long"));

        assertThatThrownBy(() -> auditoria.enviar(ana, "c1", "x", List.of()))
                .isInstanceOf(DadosInvalidosException.class);

        assertThat(linhas).singleElement().asString().contains("resultado=recusado");
    }
}
