package br.com.fademy.infrastructure.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean(destroyMethod = "desconectar")
    public PostgresAdapter postgresAdapter() {
        return DatabaseManager.getInstance().getPostgresAdapter();
    }

    @Bean(destroyMethod = "desconectar")
    public MongoAdapter mongoAdapter() {
        return DatabaseManager.getInstance().getMongoAdapter();
    }

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
