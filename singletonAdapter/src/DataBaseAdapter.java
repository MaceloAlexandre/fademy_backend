public interface DataBaseAdapter {
    void conectar();

    void desconectar();

    void salvarUsuario(String nome);
}
