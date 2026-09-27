package br.com.fademy.infrastructure.database;

import br.com.fademy.IntegracaoTest;
import com.mongodb.client.MongoClient;
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

    @Test
    void getInstanceDevolveSempreOMesmoObjeto() {
        assertThat(DatabaseManager.getInstance()).isSameAs(DatabaseManager.getInstance());
    }

    @Test
    void springUsaAsConexoesDoSingleton() {
        assertThat(dataSource).isSameAs(DatabaseManager.getInstance().getPostgres());
        assertThat(mongoClient).isSameAs(DatabaseManager.getInstance().getMongo());
    }

    @Test
    void flywayRodaNaSubida() {
        Boolean existe = new JdbcTemplate(dataSource).queryForObject(
                "select exists (select from information_schema.tables where table_name = 'flyway_schema_history')",
                Boolean.class);
        assertThat(existe).isTrue();
    }
}
