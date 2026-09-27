package br.com.fademy.application.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LimitesTest {

    @ParameterizedTest
    @CsvSource(value = {"NULL, 20", "abc, 20", "0, 20", "-3, 20", "7, 7", "50, 50", "51, 50", "' 9 ', 9"},
            nullValues = "NULL")
    void limitePadraoVinteMaximoCinquenta(String pedido, int esperado) {
        assertThat(Limites.ler(pedido, 20, 50)).isEqualTo(esperado);
    }
}
