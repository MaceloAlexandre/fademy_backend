package br.com.fademy.infrastructure.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Adapter do MongoDB: guarda o cliente e o banco usados pelos gateways do Mongo, e grava os
 * arquivos (imagens) no GridFS, bucket "imagens" (#62).
 */
public class MongoAdapter implements DataBaseAdapter {

    private final String uri;
    private final int timeoutSegundos;

    private MongoClient cliente;
    private MongoDatabase banco;
    private GridFSBucket arquivos;

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
        arquivos = GridFSBuckets.create(banco, "imagens");
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

    /** Grava o arquivo no GridFS com o nome dado e o tipo nos metadados. */
    public void salvarArquivo(String nome, byte[] conteudo, String tipo) {
        arquivos.uploadFromStream(nome, new ByteArrayInputStream(conteudo),
                new GridFSUploadOptions().metadata(new Document("tipo", tipo)));
    }

    public boolean existeArquivo(String nome) {
        return arquivos.find(Filters.eq("filename", nome)).first() != null;
    }

    public Optional<Arquivo> lerArquivo(String nome) {
        GridFSFile encontrado = arquivos.find(Filters.eq("filename", nome)).first();
        if (encontrado == null) {
            return Optional.empty();
        }
        ByteArrayOutputStream conteudo = new ByteArrayOutputStream();
        arquivos.downloadToStream(encontrado.getObjectId(), conteudo);
        return Optional.of(new Arquivo(conteudo.toByteArray(), encontrado.getMetadata().getString("tipo")));
    }

    public record Arquivo(byte[] conteudo, String tipo) {
    }
}
