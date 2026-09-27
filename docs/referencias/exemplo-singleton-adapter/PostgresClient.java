public class PostgresClient {
    private final String host;
    private final int porta;
    private final String database;

    public PostgresClient(String host, int porta, String database){
        this.host = host;
        this.porta = porta;
        this.database = database;
    }

    public void conectar(){
        System.out.println(
                "Conetando ao PortgresSQL em "
                    + host + ":" + porta +
                        "/" + database
        );
    }

    public void desconectar(){
        System.out.println("Desconectando do PostgresSQL");
    }

    public void inserirUsuario(String nome){
        System.out.println(
                "PostgresSQL executou: INSERT INTO usuario (nome) VALUES ('"
                    + nome + "')"
        );
    }
}
