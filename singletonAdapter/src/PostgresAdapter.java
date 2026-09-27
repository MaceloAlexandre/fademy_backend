public class PostgresAdapter implements DataBaseAdapter {
    private final PostgresClient postgresClient;

    public PostgresAdapter(PostgresClient postgresClient) {
        this.postgresClient = postgresClient;
    }

    @Override
    public void conectar() {
        postgresClient.conectar();
    }

    @Override
    public void desconectar() {
        postgresClient.desconectar();
    }

    @Override
    public void salvarUsuario(String nome) {
        postgresClient.inserirUsuario(nome);
    }
}
