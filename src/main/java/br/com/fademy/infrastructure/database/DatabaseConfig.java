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

    @Bean
    public DatabaseManager databaseManager() {
        return DatabaseManager.getInstance();
    }

    // destroyMethod vazio: as conexões são do Singleton, não do contexto do Spring.
    @Bean(destroyMethod = "")
    public DataSource dataSource(DatabaseManager databaseManager) {
        return databaseManager.getPostgres();
    }

    @Bean(destroyMethod = "")
    public MongoClient mongoClient(DatabaseManager databaseManager) {
        return databaseManager.getMongo();
    }

    @Bean
    public MongoDatabase mongoDatabase(DatabaseManager databaseManager) {
        return databaseManager.getMongoDatabase();
    }
}
