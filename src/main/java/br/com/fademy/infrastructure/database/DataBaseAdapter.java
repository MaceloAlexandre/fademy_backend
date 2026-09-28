package br.com.fademy.infrastructure.database;

/**
 * Adapter de um banco, como no exemplo do professor (docs/referencias/exemplo-singleton-adapter).
 * O {@link DatabaseManager} cria um adapter por banco e os entrega.
 */
public interface DataBaseAdapter {

    void conectar();

    void desconectar();
}
