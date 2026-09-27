package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.SeguirGateway;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BloqueioWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    SeguirGateway seguires;

    @Autowired
    BloqueioGateway bloqueios;

    @Autowired
    MongoDatabase mongo;

    @Autowired
    JdbcClient jdbc;

    @Test
    void bloquearApagaFollowsEPedidosNosDoisBancos() throws Exception {
        Usuario ana = cenario.privado(Papel.ALUNO);
        Usuario bia = cenario.privado(Papel.ALUNO);
        seguir(ana, bia);
        seguir(bia, ana);
        assertThat(pedidosEntre(ana, bia)).isEqualTo(2);

        bloquear(ana, bia)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"blocked\":true}", true));

        assertThat(seguires.buscar(ana.id(), bia.id())).isEmpty();
        assertThat(seguires.buscar(bia.id(), ana.id())).isEmpty();
        assertThat(pedidosEntre(ana, bia)).isZero();
        assertThat(bloqueios.existeEntre(ana.id(), bia.id())).isTrue();
    }

    @Test
    void bloquearDeNovoNaoDuplica() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);

        bloquear(ana, bia).andExpect(status().isOk());
        bloquear(ana, bia).andExpect(status().isOk());

        assertThat(jdbc.sql("select count(*) from bloqueio where bloqueador_id = :a and bloqueado_id = :b")
                .param("a", UUID.fromString(ana.id())).param("b", UUID.fromString(bia.id()))
                .query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void validacoes() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario deOutra = cenario.usuario(Papel.ALUNO, cenario.instituicao("outra-faculdade"));

        erro(bloquear(ana, ana.id()), 400, "Cannot block yourself");
        erro(bloquear(ana, deOutra.id()), 404, "User not found");
        erro(bloquear(ana, UUID.randomUUID().toString()), 404, "User not found");
        erro(bloquear(ana, "id-invalido"), 404, "User not found");
        erro(desbloquear(ana, ana.id()), 400, "Cannot unblock yourself");
    }

    @Test
    void desbloquearNaoDevolveOsFollows() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        seguir(ana, bia);
        bloquear(ana, bia);

        for (int i = 0; i < 2; i++) {
            desbloquear(ana, bia.id())
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"unblocked\":true}", true));
        }

        assertThat(bloqueios.existeEntre(ana.id(), bia.id())).isFalse();
        assertThat(seguires.buscar(ana.id(), bia.id())).isEmpty();
    }

    @Test
    void comBloqueioNinguemSegueNinguem() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        bloquear(ana, bia);

        erro(seguir(bia, ana), 403, "Forbidden");
        erro(seguir(ana, bia), 403, "Forbidden");
    }

    private long pedidosEntre(Usuario a, Usuario b) {
        return mongo.getCollection("notificacoes").countDocuments(Filters.and(
                Filters.eq("tipo", "follow_request"),
                Filters.in("atorId", a.id(), b.id()),
                Filters.in("destinatarioId", a.id(), b.id())));
    }

    private void erro(ResultActions resposta, int status, String mensagem) throws Exception {
        resposta.andExpect(status().is(status))
                .andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private ResultActions seguir(Usuario quem, Usuario alvo) throws Exception {
        return mvc.perform(post("/api/users/" + alvo.id() + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private ResultActions bloquear(Usuario quem, Usuario alvo) throws Exception {
        return bloquear(quem, alvo.id());
    }

    private ResultActions bloquear(Usuario quem, String alvo) throws Exception {
        return mvc.perform(post("/api/users/" + alvo + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private ResultActions desbloquear(Usuario quem, String alvo) throws Exception {
        return mvc.perform(delete("/api/users/" + alvo + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
