package br.com.fademy.infrastructure.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguracaoBancosTest {

    private static final String[] CHAVES = {"POSTGRES_URL", "POSTGRES_USER", "POSTGRES_PASSWORD", "MONGO_URI"};
    private final Map<String, String> salvas = new HashMap<>();

    @BeforeEach
    void limpar() {
        for (String chave : CHAVES) {
            salvas.put(chave, System.getProperty(chave));
            System.clearProperty(chave);
        }
    }

    @AfterEach
    void restaurar() {
        salvas.forEach((chave, valor) -> {
            if (valor != null) {
                System.setProperty(chave, valor);
            }
        });
    }

    @Test
    void semConfiguracaoUsaOsBancosDoDockerCompose() {
        ConfiguracaoBancos config = ConfiguracaoBancos.doAmbiente();

        assertThat(config.postgresUrl()).isEqualTo("jdbc:postgresql://localhost:5433/fademy");
        assertThat(config.postgresUsuario()).isEqualTo("fademy");
        assertThat(config.postgresSenha()).isEqualTo("fademy");
        assertThat(config.mongoUri()).isEqualTo("mongodb://localhost:27017/fademy");
    }

    @Test
    void propriedadeDeSistemaSobrepoeOPadrao() {
        System.setProperty("MONGO_URI", "mongodb://outro:27017/x");

        assertThat(ConfiguracaoBancos.doAmbiente().mongoUri()).isEqualTo("mongodb://outro:27017/x");
    }
}
