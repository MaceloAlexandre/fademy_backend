package br.com.fademy.infrastructure.database;

import br.com.fademy.IntegracaoTest;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseManagerTest extends IntegracaoTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    MongoDatabase mongoDatabase;

    @Test
    void getInstanceDevolveSempreOMesmoObjeto() {
        assertThat(DatabaseManager.getInstance()).isSameAs(DatabaseManager.getInstance());
    }

    @Test
    void entregaOsAdaptersJaConectados() {
        DatabaseManager manager = DatabaseManager.getInstance();

        assertThat(manager.getPostgresAdapter().getDataSource()).isNotNull();
        assertThat(manager.getMongoAdapter().getBanco()).isNotNull();
    }

    @Test
    void springUsaAsConexoesDosAdaptersDoSingleton() {
        DatabaseManager manager = DatabaseManager.getInstance();

        assertThat(dataSource).isSameAs(manager.getPostgresAdapter().getDataSource());
        assertThat(mongoClient).isSameAs(manager.getMongoAdapter().getCliente());
        assertThat(mongoDatabase).isSameAs(manager.getMongoAdapter().getBanco());
    }

    @Test
    void flywayRodaNaSubida() {
        Boolean existe = new JdbcTemplate(dataSource).queryForObject(
                "select exists (select from information_schema.tables where table_name = 'flyway_schema_history')",
                Boolean.class);
        assertThat(existe).isTrue();
    }
}
