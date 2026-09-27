package br.com.fademy.infrastructure.mongo;

import br.com.fademy.application.gateway.SaudeGateway;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import org.bson.Document;

public class MongoSaudeGateway implements SaudeGateway {

    private final MongoClient mongo;

    public MongoSaudeGateway(MongoClient mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean estaNoAr() {
        try {
            mongo.getDatabase("admin").runCommand(new Document("ping", 1));
            return true;
        } catch (MongoException e) {
            return false;
        }
    }
}
