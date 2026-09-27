package br.com.fademy.infrastructure.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Singleton que guarda as conexões com o PostgreSQL e o MongoDB, no formato do exemplo do
 * professor (docs/referencias/exemplo-singleton-adapter). Só o {@link DatabaseConfig} chama
 * {@link #getInstance()}, uma vez, na subida da aplicação (docs/ARQUITETURA.md, seção 6).
 */
public class DatabaseManager {

    // Tempo máximo esperando um banco responder, para o /health não travar com banco fora.
    private static final int TIMEOUT_SEGUNDOS = 2;

    private static DatabaseManager instance;

    private final DataSource postgres;
    private final MongoClient mongo;

    private DatabaseManager() {
        ConfiguracaoBancos config = ConfiguracaoBancos.doAmbiente();

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.postgresUrl());
        hikari.setUsername(config.postgresUsuario());
        hikari.setPassword(config.postgresSenha());
        hikari.setConnectionTimeout(TimeUnit.SECONDS.toMillis(TIMEOUT_SEGUNDOS));
        postgres = new HikariDataSource(hikari);

        mongo = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(config.mongoUri()))
                .applyToClusterSettings(c -> c.serverSelectionTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS))
                .build());
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    public DataSource getPostgres() {
        return postgres;
    }

    public MongoClient getMongo() {
        return mongo;
    }
}
