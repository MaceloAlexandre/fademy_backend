package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SeguirWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    SeguirGateway seguires;

    @Autowired
    MongoDatabase mongo;

    @Autowired
    JdbcClient jdbc;

    @Test
    void seguirPerfilPublico() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);

        seguir(ana, bia.id())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.followId").isString());

        assertThat(seguires.buscar(ana.id(), bia.id()).orElseThrow().status()).isEqualTo(StatusSeguir.ACCEPTED);
        assertThat(notificacoes(ana, bia, "new_follower")).isEqualTo(1);
    }

    @Test
    void seguirPerfilPrivadoCriaPedidoSemDuplicarNotificacao() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.privado(Papel.PROFESSOR);

        seguir(ana, bia.id()).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("pending"));
        deixarDeSeguir(ana, bia.id()).andExpect(status().isOk());
        seguir(ana, bia.id()).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("pending"));

        assertThat(notificacoes(ana, bia, "follow_request")).isEqualTo(1);
    }

    @Test
    void validacoesNaOrdemDaRN21() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        Usuario bloqueadora = cenario.usuario(Papel.ALUNO);
        Usuario deOutra = cenario.usuario(Papel.ALUNO, cenario.instituicao("outra-faculdade"));
        bloquear(bloqueadora, ana);

        erro(ana, ana.id(), 400, "Cannot follow yourself");
        erro(ana, bloqueadora.id(), 403, "Forbidden");
        erro(ana, deOutra.id(), 404, "User not found");
        erro(ana, UUID.randomUUID().toString(), 404, "User not found");
        erro(ana, "id-invalido", 404, "User not found");

        seguir(ana, bia.id()).andExpect(status().isCreated());
        erro(ana, bia.id(), 409, "Already following or request pending");
    }

    @Test
    void deixarDeSeguirEhIdempotente() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        seguir(ana, bia.id());

        for (int i = 0; i < 2; i++) {
            deixarDeSeguir(ana, bia.id())
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"unfollowed\":true}", true));
        }
        assertThat(seguires.buscar(ana.id(), bia.id())).isEmpty();
    }

    private long notificacoes(Usuario ator, Usuario destinatario, String tipo) {
        return mongo.getCollection("notificacoes").countDocuments(Filters.and(
                Filters.eq("tipo", tipo), Filters.eq("atorId", ator.id()), Filters.eq("destinatarioId", destinatario.id())));
    }

    private void bloquear(Usuario bloqueador, Usuario bloqueado) {
        jdbc.sql("insert into bloqueio (id, bloqueador_id, bloqueado_id, criado_em) values (:id, :a, :b, :agora)")
                .param("id", UUID.randomUUID())
                .param("a", UUID.fromString(bloqueador.id()))
                .param("b", UUID.fromString(bloqueado.id()))
                .param("agora", Timestamp.from(Instant.now()))
                .update();
    }

    private void erro(Usuario quem, String alvo, int status, String mensagem) throws Exception {
        seguir(quem, alvo)
                .andExpect(status().is(status))
                .andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private ResultActions seguir(Usuario quem, String alvo) throws Exception {
        return mvc.perform(post("/api/users/" + alvo + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private ResultActions deixarDeSeguir(Usuario quem, String alvo) throws Exception {
        return mvc.perform(delete("/api/users/" + alvo + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
