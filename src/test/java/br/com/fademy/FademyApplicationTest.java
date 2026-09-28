package br.com.fademy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class FademyApplicationTest extends IntegracaoTest {

    @Autowired
    private Environment ambiente;

    @Test
    void contextoSobe() {
    }

    @Test
    void semPortConfiguradaAApiUsaA8080() {
        assertThat(ambiente.getProperty("server.port")).isEqualTo("8080");
    }
}
