package br.com.fademy.infrastructure.database;

import br.com.fademy.IntegracaoTest;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Usa os bancos do Testcontainers, com adapters próprios para não fechar os do Singleton.
class AdaptersTest extends IntegracaoTest {

    @Test
    void postgresAdapterConectaEDesconecta() {
        PostgresAdapter adapter = new PostgresAdapter(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2);

        adapter.conectar();
        assertThat(((HikariDataSource) adapter.getDataSource()).isClosed()).isFalse();

        adapter.desconectar();
        assertThat(((HikariDataSource) adapter.getDataSource()).isClosed()).isTrue();
    }

    @Test
    void mongoAdapterConectaEDesconecta() {
        MongoAdapter adapter = new MongoAdapter(MONGO.getReplicaSetUrl("fademy"), 2);

        adapter.conectar();
        assertThat(adapter.getBanco().getName()).isEqualTo("fademy");

        adapter.desconectar();
        assertThatThrownBy(() -> adapter.getBanco().listCollectionNames().first())
                .isInstanceOf(IllegalStateException.class);
    }
}
