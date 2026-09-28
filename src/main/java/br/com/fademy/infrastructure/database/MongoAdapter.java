package br.com.fademy.infrastructure.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.concurrent.TimeUnit;

/**
 * Adapter do MongoDB: guarda o cliente e o banco usados pelos gateways do Mongo.
 */
public class MongoAdapter implements DataBaseAdapter {

    private final String uri;
    private final int timeoutSegundos;

    private MongoClient cliente;
    private MongoDatabase banco;

    public MongoAdapter(String uri, int timeoutSegundos) {
        this.uri = uri;
        this.timeoutSegundos = timeoutSegundos;
    }

    @Override
    public void conectar() {
        ConnectionString conexao = new ConnectionString(uri);
        cliente = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(conexao)
                .applyToClusterSettings(c -> c.serverSelectionTimeout(timeoutSegundos, TimeUnit.SECONDS))
                .build());
        banco = cliente.getDatabase(conexao.getDatabase() != null ? conexao.getDatabase() : "fademy");
    }

    @Override
    public void desconectar() {
        cliente.close();
    }

    public MongoClient getCliente() {
        return cliente;
    }

    /** O banco indicado na MONGO_URI (padrão "fademy"). */
    public MongoDatabase getBanco() {
        return banco;
    }
}
