package br.com.fademy.infrastructure.mongo;

import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.domain.notificacao.Notificacao;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Coleção "notificacoes". Guarda só os ids do Postgres (destinatário, ator, referência);
 * os dados do usuário são lidos na hora de listar (SPEC RN48).
 */
@Component
public class MongoNotificacaoGateway implements NotificacaoGateway {

    private static final Logger log = LoggerFactory.getLogger(MongoNotificacaoGateway.class);

    private final MongoCollection<Document> colecao;

    public MongoNotificacaoGateway(MongoDatabase mongo) {
        this.colecao = mongo.getCollection("notificacoes");
    }

    @Override
    public void criar(Notificacao n) {
        try {
            colecao.insertOne(new Document()
                    .append("destinatarioId", n.destinatarioId())
                    .append("atorId", n.atorId())
                    .append("tipo", n.tipo().texto())
                    .append("refId", n.refId())
                    .append("refModelo", n.tipo().modeloReferencia())
                    .append("lida", n.lida())
                    .append("criadaEm", Date.from(n.criadaEm())));
        } catch (MongoException e) {
            // SPEC RN37: a notificação nunca derruba a ação principal.
            log.error("Falha ao gravar notificação {} para {}", n.tipo().texto(), n.destinatarioId(), e);
        }
    }

    @Override
    public void removerPedidoDeSeguir(String atorId, String destinatarioId) {
        try {
            colecao.deleteOne(Filters.and(
                    Filters.eq("tipo", "follow_request"),
                    Filters.eq("atorId", atorId),
                    Filters.eq("destinatarioId", destinatarioId)));
        } catch (MongoException e) {
            log.error("Falha ao apagar pedido de seguir de {} para {}", atorId, destinatarioId, e);
        }
    }

    @Override
    public void removerPedidosDeSeguirEntre(String usuarioA, String usuarioB) {
        try {
            colecao.deleteMany(Filters.and(
                    Filters.eq("tipo", "follow_request"),
                    Filters.or(
                            Filters.and(Filters.eq("atorId", usuarioA), Filters.eq("destinatarioId", usuarioB)),
                            Filters.and(Filters.eq("atorId", usuarioB), Filters.eq("destinatarioId", usuarioA)))));
        } catch (MongoException e) {
            log.error("Falha ao apagar pedidos de seguir entre {} e {}", usuarioA, usuarioB, e);
        }
    }
}
