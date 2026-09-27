package br.com.fademy.infrastructure.mongo;

import br.com.fademy.application.gateway.ConversaGateway;
import br.com.fademy.domain.conversa.Conversa;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Coleção "conversas". O campo "par" (os dois ids em ordem) tem índice único: é ele que garante
 * uma conversa por par de usuários.
 */
@Component
public class MongoConversaGateway implements ConversaGateway {

    private static final Logger log = LoggerFactory.getLogger(MongoConversaGateway.class);

    private final MongoCollection<Document> colecao;

    public MongoConversaGateway(MongoDatabase mongo) {
        this.colecao = mongo.getCollection("conversas");
        try {
            colecao.createIndex(Indexes.ascending("par"), new IndexOptions().unique(true));
        } catch (MongoException e) {
            // Com o MongoDB fora do ar a aplicação sobe mesmo assim (o /health mostra "down").
            log.warn("Não foi possível criar o índice único de conversas agora", e);
        }
    }

    @Override
    public Resultado buscarOuCriar(Conversa c) {
        String par = par(c.membros());
        boolean criada;
        try {
            criada = colecao.updateOne(Filters.eq("par", par), Updates.combine(
                    Updates.setOnInsert("membros", c.membros()),
                    Updates.setOnInsert("criadaPorId", c.criadaPorId()),
                    Updates.setOnInsert("criadaEm", Date.from(c.criadaEm())),
                    Updates.setOnInsert("atualizadaEm", Date.from(c.atualizadaEm()))),
                    new UpdateOptions().upsert(true)).getUpsertedId() != null;
        } catch (MongoWriteException e) {
            // Outro pedido criou a mesma conversa no mesmo instante: vale a dele.
            if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
                throw e;
            }
            criada = false;
        }
        return new Resultado(conversa(colecao.find(Filters.eq("par", par)).first()), criada);
    }

    @Override
    public Optional<Conversa> buscarPorId(String id) {
        if (!ObjectId.isValid(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(colecao.find(Filters.eq("_id", new ObjectId(id))).first())
                .map(MongoConversaGateway::conversa);
    }

    @Override
    public void registrarAtividade(String conversaId, Instant quando) {
        colecao.updateOne(Filters.eq("_id", new ObjectId(conversaId)),
                Updates.set("atualizadaEm", Date.from(quando)));
    }

    @Override
    public List<Conversa> doUsuario(String usuarioId) {
        return colecao.find(Filters.eq("membros", usuarioId)).sort(Sorts.descending("atualizadaEm"))
                .map(MongoConversaGateway::conversa)
                .into(new ArrayList<>());
    }

    private static String par(List<String> membros) {
        return membros.stream().sorted().reduce((a, b) -> a + "|" + b).orElseThrow();
    }

    static Conversa conversa(Document d) {
        return new Conversa(d.getObjectId("_id").toHexString(), d.getList("membros", String.class),
                d.getString("criadaPorId"), d.getDate("criadaEm").toInstant(), d.getDate("atualizadaEm").toInstant());
    }
}
