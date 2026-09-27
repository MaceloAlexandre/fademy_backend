package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PedidoDeSeguirWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    SeguirGateway seguires;

    @Autowired
    MongoDatabase mongo;

    @Test
    void aceitarPedido() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.privado(Papel.ALUNO);
        String pedido = pedir(ana, bia);

        responder(bia, pedido, "accept")
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"accepted\"}", true));

        assertThat(seguires.buscarPorId(pedido).orElseThrow().status()).isEqualTo(StatusSeguir.ACCEPTED);
        assertThat(mongo.getCollection("notificacoes").countDocuments(Filters.and(
                Filters.eq("tipo", "follow_accepted"), Filters.eq("destinatarioId", ana.id()),
                Filters.eq("atorId", bia.id()), Filters.eq("refId", pedido)))).isEqualTo(1);
    }

    @Test
    void rejeitarPedidoApagaOFollow() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.privado(Papel.ALUNO);
        String pedido = pedir(ana, bia);

        responder(bia, pedido, "reject")
                .andExpect(status().isOk())
                .andExpect(content().json("{\"rejected\":true}", true));

        assertThat(seguires.buscarPorId(pedido)).isEmpty();
    }

    @Test
    void pedidoInexistente() throws Exception {
        Usuario bia = cenario.usuario(Papel.ALUNO);

        for (String acao : new String[]{"accept", "reject"}) {
            for (String id : new String[]{UUID.randomUUID().toString(), "id-invalido"}) {
                responder(bia, id, acao)
                        .andExpect(status().isNotFound())
                        .andExpect(content().json("{\"error\":\"Follow request not found\"}", true));
            }
        }
    }

    @Test
    void soQuemRecebeuOPedidoResponde() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.privado(Papel.ALUNO);
        Usuario carla = cenario.usuario(Papel.ALUNO);
        String pedido = pedir(ana, bia);

        for (Usuario quem : new Usuario[]{ana, carla}) {
            for (String acao : new String[]{"accept", "reject"}) {
                responder(quem, pedido, acao)
                        .andExpect(status().isForbidden())
                        .andExpect(content().json("{\"error\":\"Forbidden\"}", true));
            }
        }
        Seguir intacto = seguires.buscarPorId(pedido).orElseThrow();
        assertThat(intacto.status()).isEqualTo(StatusSeguir.PENDING);
    }

    @Test
    void naoAceitaFollowJaAceito() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        String follow = pedir(ana, bia);

        responder(bia, follow, "accept")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"No pending request\"}", true));
    }

    private String pedir(Usuario quem, Usuario alvo) throws Exception {
        String resposta = mvc.perform(post("/api/users/" + alvo.id() + "/follow")
                        .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.followId");
    }

    private ResultActions responder(Usuario quem, String pedido, String acao) throws Exception {
        return mvc.perform(patch("/api/follows/" + pedido + "/" + acao)
                .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
