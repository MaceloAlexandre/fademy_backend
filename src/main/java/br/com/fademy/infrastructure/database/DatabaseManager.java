package br.com.fademy.infrastructure.database;

/**
 * Singleton que cria e entrega os adapters do PostgreSQL e do MongoDB, no formato do exemplo
 * do professor (docs/referencias/exemplo-singleton-adapter). Só o {@link DatabaseConfig} chama
 * {@link #getInstance()}, uma vez, na subida da aplicação (docs/ARQUITETURA.md, seção 6).
 */
public class DatabaseManager {

    // Tempo máximo esperando um banco responder, para o /health não travar com banco fora.
    private static final int TIMEOUT_SEGUNDOS = 2;

    private static DatabaseManager instance;

    private final PostgresAdapter postgresAdapter;
    private final MongoAdapter mongoAdapter;

    private DatabaseManager() {
        ConfiguracaoBancos config = ConfiguracaoBancos.doAmbiente();

        postgresAdapter = new PostgresAdapter(
                config.postgresUrl(), config.postgresUsuario(), config.postgresSenha(), TIMEOUT_SEGUNDOS);
        mongoAdapter = new MongoAdapter(config.mongoUri(), TIMEOUT_SEGUNDOS);

        postgresAdapter.conectar();
        mongoAdapter.conectar();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    public PostgresAdapter getPostgresAdapter() {
        return postgresAdapter;
    }

    public MongoAdapter getMongoAdapter() {
        return mongoAdapter;
    }
}
