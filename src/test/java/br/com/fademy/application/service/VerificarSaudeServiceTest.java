package br.com.fademy.application.service;

import br.com.fademy.application.usecase.VerificarSaudeUseCase.Saude;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificarSaudeServiceTest {

    @Test
    void doisBancosNoArEstaTudoNoAr() {
        Saude saude = new VerificarSaudeService(() -> true, () -> true).executar();

        assertThat(saude).isEqualTo(new Saude(true, true));
        assertThat(saude.tudoNoAr()).isTrue();
    }

    @Test
    void umBancoForaNaoEstaTudoNoAr() {
        assertThat(new VerificarSaudeService(() -> true, () -> false).executar().tudoNoAr()).isFalse();
        assertThat(new VerificarSaudeService(() -> false, () -> true).executar().tudoNoAr()).isFalse();
    }
}
