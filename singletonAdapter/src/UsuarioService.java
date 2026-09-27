import javax.xml.crypto.Data;

public class UsuarioService {
    private final DataBaseAdapter databaseAdapter;
    public UsuarioService(DataBaseAdapter dataBaseAdapter) {
        this.databaseAdapter = dataBaseAdapter;
    }

    public void cadastrarUsuario(String nome) {
        System.out.println("Cadastro usando Adpater e Singleton");
        databaseAdapter.conectar();
        try {
            databaseAdapter.salvarUsuario(nome);
        } finally {
            databaseAdapter.desconectar();
        }
    }
}
