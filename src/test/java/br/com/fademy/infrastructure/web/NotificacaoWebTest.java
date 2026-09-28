package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class NotificacaoWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    UsuarioGateway usuarios;

    @Test
    void listaComAtorEStatusAtualDosPedidos() throws Exception {
        Usuario bia = cenario.privado(Papel.PROFESSOR);
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario carla = cenario.usuario(Papel.ALUNO);
        Usuario duda = cenario.usuario(Papel.ALUNO);
        seguir(ana, bia);                                   // pedido que fica pendente
        String pedidoDaCarla = seguir(carla, bia);          // pedido que será aceito
        String pedidoDaDuda = seguir(duda, bia);            // pedido que será rejeitado
        responder(bia, pedidoDaCarla, "accept");
        responder(bia, pedidoDaDuda, "reject");

        listar(bia, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications", hasSize(3)))
                .andExpect(jsonPath("$.notifications[*].actorId.name",
                        contains("Usuário aluno", "Usuário aluno", "Usuário aluno")))
                .andExpect(jsonPath("$.notifications[*].actorId._id", contains(duda.id(), carla.id(), ana.id())))
                .andExpect(jsonPath("$.notifications[*].followStatus", contains(null, "accepted", "pending")))
                .andExpect(jsonPath("$.notifications[0].userId").value(bia.id()))
                .andExpect(jsonPath("$.notifications[0].type").value("follow_request"))
                .andExpect(jsonPath("$.notifications[0].refId").value(pedidoDaDuda))
                .andExpect(jsonPath("$.notifications[0].refModel").value("Follow"))
                .andExpect(jsonPath("$.notifications[0].read").value(false))
                .andExpect(jsonPath("$.notifications[0].createdAt").isString())
                .andExpect(jsonPath("$.unreadCount").value(3))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.nextCursor").value((Object) null));

        // Outros tipos não têm followStatus.
        listar(carla, "").andExpect(jsonPath("$.notifications[0].type").value("follow_accepted"))
                .andExpect(jsonPath("$.notifications[0].followStatus").doesNotExist());
    }

    @Test
    void paginacaoPorCursorEContagemDeNaoLidas() throws Exception {
        Usuario bia = cenario.usuario(Papel.PROFESSOR);
        List<Usuario> seguidores = List.of(cenario.usuario(Papel.ALUNO), cenario.usuario(Papel.ALUNO),
                cenario.usuario(Papel.ALUNO));
        for (Usuario s : seguidores) {
            seguir(s, bia);
        }

        String primeira = listar(bia, "?limit=2")
                .andExpect(jsonPath("$.notifications[*].actorId._id",
                        contains(seguidores.get(2).id(), seguidores.get(1).id())))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.unreadCount").value(3))
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(primeira, "$.nextCursor");

        listar(bia, "?limit=2&cursor=" + cursor)
                .andExpect(jsonPath("$.notifications[*].actorId._id", contains(seguidores.get(0).id())))
                .andExpect(jsonPath("$.hasMore").value(false));
        listar(bia, "?cursor=nao-e-cursor")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Invalid cursor\"}", true));
    }

    @Test
    void fotoNovaDoAtorApareceEmNotificacaoAntiga() throws Exception {
        Usuario bia = cenario.usuario(Papel.PROFESSOR);
        Usuario ana = cenario.usuario(Papel.ALUNO);
        seguir(ana, bia);

        ana.trocarAvatar("avatars/foto-nova.png");
        usuarios.atualizar(ana);

        listar(bia, "").andExpect(jsonPath("$.notifications[0].actorId.avatarUrl").value(url("avatars/foto-nova.png")));
    }

    @Test
    void marcarComoLidaEExcluir() throws Exception {
        Usuario bia = cenario.usuario(Papel.PROFESSOR);
        Usuario ana = cenario.usuario(Papel.ALUNO);
        seguir(ana, bia);
        seguir(cenario.usuario(Papel.ALUNO), bia);
        String id = JsonPath.read(listar(bia, "").andReturn().getResponse().getContentAsString(),
                "$.notifications[0]._id");

        mvc.perform(patch("/api/notifications/" + id + "/read").header(HttpHeaders.AUTHORIZATION, cenario.bearer(bia)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"read\":true}", true));
        listar(bia, "").andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].read").value(true));

        mvc.perform(delete("/api/notifications/" + id).header(HttpHeaders.AUTHORIZATION, cenario.bearer(bia)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"deleted\":true}", true));
        listar(bia, "").andExpect(jsonPath("$.notifications", hasSize(1)));
    }

    @Test
    void notificacaoDeOutroOuInexistente() throws Exception {
        Usuario bia = cenario.usuario(Papel.PROFESSOR);
        Usuario ana = cenario.usuario(Papel.ALUNO);
        seguir(ana, bia);
        String daBia = JsonPath.read(listar(bia, "").andReturn().getResponse().getContentAsString(),
                "$.notifications[0]._id");
        String bearerDaAna = cenario.bearer(ana);

        mvc.perform(patch("/api/notifications/" + daBia + "/read").header(HttpHeaders.AUTHORIZATION, bearerDaAna))
                .andExpect(status().isForbidden()).andExpect(content().json("{\"error\":\"Forbidden\"}", true));
        mvc.perform(delete("/api/notifications/" + daBia).header(HttpHeaders.AUTHORIZATION, bearerDaAna))
                .andExpect(status().isForbidden());
        for (String id : new String[]{"66f6f6f6f6f6f6f6f6f6f6f6", "id-invalido"}) {
            mvc.perform(patch("/api/notifications/" + id + "/read").header(HttpHeaders.AUTHORIZATION, bearerDaAna))
                    .andExpect(status().isNotFound())
                    .andExpect(content().json("{\"error\":\"Notification not found\"}", true));
            mvc.perform(delete("/api/notifications/" + id).header(HttpHeaders.AUTHORIZATION, bearerDaAna))
                    .andExpect(status().isNotFound());
        }
        assertThat(JsonPath.<Integer>read(listar(bia, "").andReturn().getResponse().getContentAsString(),
                "$.unreadCount")).isEqualTo(1);
    }

    private ResultActions listar(Usuario quem, String parametros) throws Exception {
        return mvc.perform(get("/api/notifications" + parametros).header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private String seguir(Usuario quem, Usuario alvo) throws Exception {
        String resposta = mvc.perform(post("/api/users/" + alvo.id() + "/follow")
                .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem))).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.followId");
    }

    private void responder(Usuario quem, String pedido, String acao) throws Exception {
        mvc.perform(patch("/api/follows/" + pedido + "/" + acao).header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
