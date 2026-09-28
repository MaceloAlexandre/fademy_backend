package br.com.fademy.infrastructure.database;

public class DatabaseManager {

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
