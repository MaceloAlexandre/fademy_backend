public class MongoClient {
    private final String host;
    private final int porta;
    private final String database;

    public MongoClient(String host, int porta, String database) {
        this.host = host;
        this.porta = porta;
        this.database = database;
    }

    public void conectar() {
        System.out.println(
                "Conectando ao MongoDB em "
                    + host + ":" + porta +
                       "/" + database
        );
    }

    public void desconectar() {
        System.out.println("Desconectando do MongoDB");
    }

    public void salvarDocumento(String colecao, String documento) {
        System.out.println("MongoDB salvou em " + colecao + ": " + documento);
    }
}
