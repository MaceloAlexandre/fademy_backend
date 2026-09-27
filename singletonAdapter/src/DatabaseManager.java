public class DatabaseManager {
    private static DatabaseManager instance;

    private final DataBaseAdapter postgresAdapter;
    private final DataBaseAdapter mongoAdapter;

    private DatabaseManager() {
        PostgresClient postgresClient = new PostgresClient(
                "localhost", 5432, "usuarios_db"
        );
        MongoClient mongoClient = new MongoClient(
                "localhost", 27017, "auditoria_db"
        );

        postgresAdapter = new PostgresAdapter(postgresClient);
        mongoAdapter = new MongoAdapter(mongoClient);
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    public DataBaseAdapter getPostgresAdapter() {
        return postgresAdapter;
    }

    public DataBaseAdapter getMongoAdapter() {
        return mongoAdapter;
    }
}
