package br.com.fademy.infrastructure.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Único ponto que chama {@link DatabaseManager#getInstance()}. Isso acontece na subida, numa
 * única thread, antes de o servidor aceitar requisições; por isso o getInstance sem
 * synchronized é seguro (docs/ARQUITETURA.md, seção 6).
 */
@Configuration
public class DatabaseConfig {

    // No desligamento, o Spring chama desconectar() dos dois adapters.
    @Bean(destroyMethod = "desconectar")
    public PostgresAdapter postgresAdapter() {
        return DatabaseManager.getInstance().getPostgresAdapter();
    }

    @Bean(destroyMethod = "desconectar")
    public MongoAdapter mongoAdapter() {
        return DatabaseManager.getInstance().getMongoAdapter();
    }

    // destroyMethod vazio: quem fecha as conexões é o adapter.
    @Bean(destroyMethod = "")
    public DataSource dataSource(PostgresAdapter postgresAdapter) {
        return postgresAdapter.getDataSource();
    }

    @Bean(destroyMethod = "")
    public MongoClient mongoClient(MongoAdapter mongoAdapter) {
        return mongoAdapter.getCliente();
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoAdapter mongoAdapter) {
        return mongoAdapter.getBanco();
    }
}
