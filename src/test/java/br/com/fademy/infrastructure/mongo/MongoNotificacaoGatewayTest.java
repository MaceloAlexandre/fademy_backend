package br.com.fademy.infrastructure.mongo;

import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;

class MongoNotificacaoGatewayTest {

    @Test
    void mongoForaDoArNaoDerrubaQuemNotifica() {
        try (MongoClient fora = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://localhost:1"))
                .applyToClusterSettings(c -> c.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .build())) {
            MongoNotificacaoGateway gateway = new MongoNotificacaoGateway(fora.getDatabase("fademy"));

            assertThatCode(() -> gateway.criar(Notificacao.nova(
                    TipoNotificacao.INVITE_RECEIVED, "destino", "ator", "ref", Instant.now())))
                    .doesNotThrowAnyException();
        }
    }
}
