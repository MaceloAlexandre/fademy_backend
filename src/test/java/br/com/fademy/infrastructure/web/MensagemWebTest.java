package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MensagemWebTest extends IntegracaoTest {

    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, 1};

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    MongoDatabase mongo;

    @Test
    void enviarTextoEImagem() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        String conversa = conversa(ana, bia);

        mvc.perform(enviar(ana, conversa).param("text", "  olha isso  ")
                        .file(new MockMultipartFile("images", "a.jpg", "image/jpeg", FOTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message._id").isString())
                .andExpect(jsonPath("$.message.conversationId").value(conversa))
                .andExpect(jsonPath("$.message.senderId").value(ana.id()))
                .andExpect(jsonPath("$.message.text").value("olha isso"))
                .andExpect(jsonPath("$.message.mediaUrls", hasSize(1)))
                .andExpect(jsonPath("$.message.createdAt").isString());

        mvc.perform(enviar(ana, conversa).file(new MockMultipartFile("images", "b.png", "image/png", FOTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message.text").doesNotExist());

        assertThat(mongo.getCollection("notificacoes").countDocuments(Filters.and(
                Filters.eq("tipo", "message_received"), Filters.eq("destinatarioId", bia.id()),
                Filters.eq("atorId", ana.id()), Filters.eq("refId", conversa)))).isEqualTo(2);
    }

    @Test
    void validacoesNaOrdem() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        Usuario intrusa = cenario.usuario(Papel.ALUNO);
        String conversa = conversa(ana, bia);

        erro(mvc.perform(enviar(ana, "66f6f6f6f6f6f6f6f6f6f6f6").param("text", "oi")), 404, "Conversation not found");
        erro(mvc.perform(enviar(ana, "id-invalido").param("text", "oi")), 404, "Conversation not found");
        erro(mvc.perform(enviar(intrusa, conversa).param("text", "oi")), 403, "Forbidden");
        erro(mvc.perform(enviar(ana, conversa).param("text", "  ")), 400, "text or media is required");
        erro(mvc.perform(enviar(ana, conversa).file(new MockMultipartFile("images", "a.gif", "image/gif", FOTO))),
                400, "Invalid file type");
        erro(listar(intrusa, conversa, ""), 403, "Forbidden");
        erro(listar(ana, "id-invalido", ""), 404, "Conversation not found");

        mvc.perform(post("/api/users/" + bia.id() + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(ana)));
        erro(mvc.perform(enviar(bia, conversa).param("text", "ainda dá?")), 403, "Forbidden");
    }

    @Test
    void listarPaginadoDaMaisNovaParaAMaisAntiga() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        String conversa = conversa(ana, bia);
        for (int i = 1; i <= 3; i++) {
            mvc.perform(enviar(i % 2 == 0 ? bia : ana, conversa).param("text", "msg " + i));
        }

        String primeira = listar(bia, conversa, "?limit=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[*].text", contains("msg 3", "msg 2")))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn().getResponse().getContentAsString();
        listar(bia, conversa, "?limit=2&cursor=" + JsonPath.read(primeira, "$.nextCursor"))
                .andExpect(jsonPath("$.messages[*].text", contains("msg 1")))
                .andExpect(jsonPath("$.hasMore").value(false));
        erro(listar(bia, conversa, "?cursor=nao-e-cursor"), 400, "Invalid cursor");
    }

    @Test
    void conversaComMensagemNovaSobeETrazAUltima() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        Usuario carla = cenario.usuario(Papel.ALUNO);
        String comBia = conversa(ana, bia);
        String comCarla = conversa(ana, carla);

        mvc.perform(enviar(bia, comBia).param("text", "oi Ana"));

        mvc.perform(get("/api/conversations").header(HttpHeaders.AUTHORIZATION, cenario.bearer(ana)))
                .andExpect(jsonPath("$.conversations[*]._id", contains(comBia, comCarla)))
                .andExpect(jsonPath("$.conversations[0].lastMessage.text").value("oi Ana"))
                .andExpect(jsonPath("$.conversations[0].lastMessage.senderId").value(bia.id()))
                .andExpect(jsonPath("$.conversations[1].lastMessage").value((Object) null));
    }

    private void erro(ResultActions resposta, int status, String mensagem) throws Exception {
        resposta.andExpect(status().is(status)).andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private MockMultipartHttpServletRequestBuilder enviar(Usuario quem, String conversa) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/conversations/" + conversa + "/messages")
                .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem));
    }

    private ResultActions listar(Usuario quem, String conversa, String parametros) throws Exception {
        return mvc.perform(get("/api/conversations/" + conversa + "/messages" + parametros)
                .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private String conversa(Usuario a, Usuario b) throws Exception {
        seguir(a, b);
        seguir(b, a);
        return JsonPath.read(mvc.perform(post("/api/conversations").header(HttpHeaders.AUTHORIZATION, cenario.bearer(a))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + b.id() + "\"}"))
                .andReturn().getResponse().getContentAsString(), "$.conversation._id");
    }

    private void seguir(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
