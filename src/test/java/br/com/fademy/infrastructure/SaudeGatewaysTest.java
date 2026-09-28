package br.com.fademy.infrastructure;

import br.com.fademy.IntegracaoTest;
import br.com.fademy.infrastructure.mongo.MongoSaudeGateway;
import br.com.fademy.infrastructure.postgres.PostgresSaudeGateway;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SaudeGatewaysTest extends IntegracaoTest {

    private static final int PORTA_FECHADA = 1;

    @Autowired
    DataSource dataSource;

    @Autowired
    MongoClient mongoClient;

    @Test
    void postgresNoAr() {
        assertThat(new PostgresSaudeGateway(dataSource).estaNoAr()).isTrue();
    }

    @Test
    void postgresFora() {
        PGSimpleDataSource fora = new PGSimpleDataSource();
        fora.setUrl("jdbc:postgresql://localhost:" + PORTA_FECHADA + "/fademy");

        assertThat(new PostgresSaudeGateway(fora).estaNoAr()).isFalse();
    }

    @Test
    void mongoNoAr() {
        assertThat(new MongoSaudeGateway(mongoClient).estaNoAr()).isTrue();
    }

    @Test
    void mongoFora() {
        try (MongoClient fora = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://localhost:" + PORTA_FECHADA))
                .applyToClusterSettings(c -> c.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .build())) {
            assertThat(new MongoSaudeGateway(fora).estaNoAr()).isFalse();
        }
    }
}
