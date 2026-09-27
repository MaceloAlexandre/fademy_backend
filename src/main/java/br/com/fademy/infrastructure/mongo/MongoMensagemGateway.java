package br.com.fademy.infrastructure.mongo;

import br.com.fademy.application.gateway.MensagemGateway;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Coleção "mensagens". O remetente é só o id do usuário no Postgres.
 */
@Component
public class MongoMensagemGateway implements MensagemGateway {

    private static final Logger log = LoggerFactory.getLogger(MongoMensagemGateway.class);

    private final MongoCollection<Document> colecao;

    public MongoMensagemGateway(MongoDatabase mongo) {
        this.colecao = mongo.getCollection("mensagens");
        try {
            colecao.createIndex(Indexes.compoundIndex(Indexes.ascending("conversaId"), Indexes.descending("_id")));
        } catch (MongoException e) {
            log.warn("Não foi possível criar o índice de mensagens agora", e);
        }
    }

    @Override
    public Mensagem criar(Mensagem m) {
        Document documento = new Document()
                .append("conversaId", m.conversaId())
                .append("remetenteId", m.remetenteId())
                .append("texto", m.texto())
                .append("midias", m.midias())
                .append("criadaEm", Date.from(m.criadaEm()));
        colecao.insertOne(documento);
        return new Mensagem(documento.getObjectId("_id").toHexString(), m.conversaId(), m.remetenteId(), m.texto(),
                m.midias(), m.criadaEm());
    }

    @Override
    public List<Mensagem> daConversa(String conversaId, String cursor, int quantidade) {
        Bson filtro = Filters.eq("conversaId", conversaId);
        if (cursor != null && !cursor.isEmpty()) {
            if (!ObjectId.isValid(cursor)) {
                throw new DadosInvalidosException("Invalid cursor");
            }
            filtro = Filters.and(filtro, Filters.lt("_id", new ObjectId(cursor)));
        }
        return colecao.find(filtro).sort(Sorts.descending("_id")).limit(quantidade)
                .map(MongoMensagemGateway::mensagem)
                .into(new ArrayList<>());
    }

    @Override
    public Optional<Mensagem> ultimaDe(String conversaId) {
        return Optional.ofNullable(colecao.find(Filters.eq("conversaId", conversaId))
                        .sort(Sorts.descending("_id")).first())
                .map(MongoMensagemGateway::mensagem);
    }

    private static Mensagem mensagem(Document d) {
        return new Mensagem(d.getObjectId("_id").toHexString(), d.getString("conversaId"), d.getString("remetenteId"),
                d.getString("texto"), d.getList("midias", String.class), d.getDate("criadaEm").toInstant());
    }
}
