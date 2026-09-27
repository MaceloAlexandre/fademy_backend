package br.com.fademy.infrastructure.seguranca;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadeTest {

    @ParameterizedTest
    @CsvSource({"7d, PT168H", "12h, PT12H", "30m, PT30M", "45s, PT45S", "3600, PT1H"})
    void formatosDoJwtExpiresIn(String texto, Duration esperado) {
        assertThat(Validade.ler(texto)).isEqualTo(esperado);
    }
}
