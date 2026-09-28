package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.ConversaGateway.Resultado;
import br.com.fademy.application.usecase.IniciarConversaUseCase;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ConversaWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    IniciarConversaUseCase iniciarConversa;

    @Autowired
    MongoDatabase mongo;

    @Test
    void criaUmaVezEDepoisDevolveAMesma() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        mutuos(ana, bia);

        String id = JsonPath.read(iniciar(ana, bia.id())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversation.memberIds", contains(ana.id(), bia.id())))
                .andExpect(jsonPath("$.conversation.isGroup").value(false))
                .andExpect(jsonPath("$.conversation.createdById").value(ana.id()))
                .andExpect(jsonPath("$.conversation.createdAt").isString())
                .andExpect(jsonPath("$.conversation.updatedAt").isString())
                .andReturn().getResponse().getContentAsString(), "$.conversation._id");

        iniciar(ana, bia.id()).andExpect(status().isOk()).andExpect(jsonPath("$.conversation._id").value(id));
        iniciar(bia, ana.id()).andExpect(status().isOk()).andExpect(jsonPath("$.conversation._id").value(id));
    }

    @Test
    void doisPedidosSimultaneosCriamUmaConversaSo() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        mutuos(ana, bia);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        List<Future<Resultado>> resultados = new ArrayList<>();
        resultados.add(threads.submit(() -> {
            largada.await();
            return iniciarConversa.executar(new Visualizador(ana.id(), Papel.ALUNO, ana.instituicaoId()), bia.id());
        }));
        resultados.add(threads.submit(() -> {
            largada.await();
            return iniciarConversa.executar(new Visualizador(bia.id(), Papel.ALUNO, bia.instituicaoId()), ana.id());
        }));
        largada.countDown();

        Resultado primeiro = resultados.get(0).get();
        Resultado segundo = resultados.get(1).get();
        threads.shutdown();

        assertThat(primeiro.conversa().id()).isEqualTo(segundo.conversa().id());
        assertThat(primeiro.criada() ^ segundo.criada()).isTrue();
        assertThat(mongo.getCollection("conversas").countDocuments(Filters.all("membros", ana.id(), bia.id())))
                .isEqualTo(1);
    }

    @Test
    void validacoesNaOrdemDaRN38() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario ana = cenario.usuario(Papel.ALUNO, faculdade, "Ana", false);
        Usuario soEuSigo = cenario.usuario(Papel.ALUNO, faculdade, "Só Eu Sigo", false);
        Usuario bloqueada = cenario.usuario(Papel.ALUNO, faculdade, "Bloqueada", false);
        Usuario deOutra = cenario.usuario(Papel.ALUNO, cenario.instituicaoNova(), "De Outra", false);
        seguir(ana, soEuSigo);
        mutuos(ana, bloqueada);
        mvc.perform(post("/api/users/" + ana.id() + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(bloqueada)));

        erro(iniciar(ana, ana.id()), 400, "Cannot message yourself");
        erro(iniciar(ana, UUID.randomUUID().toString()), 404, "User not found");
        erro(iniciar(ana, "id-invalido"), 404, "User not found");
        erro(iniciar(ana, deOutra.id()), 403, "Forbidden");
        erro(iniciar(ana, bloqueada.id()), 403, "Forbidden");
        erro(iniciar(ana, soEuSigo.id()), 403, "Both users must follow each other to start a chat");
    }

    @Test
    void listarMinhasConversasComMembrosPopulados() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        Usuario carla = cenario.usuario(Papel.PROFESSOR);
        mutuos(ana, bia);
        mutuos(ana, carla);
        iniciar(ana, bia.id());
        iniciar(carla, ana.id());

        mvc.perform(get("/api/conversations").header(HttpHeaders.AUTHORIZATION, cenario.bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations[*].memberIds[*]._id",
                        contains(carla.id(), ana.id(), ana.id(), bia.id())))
                .andExpect(jsonPath("$.conversations[0].memberIds[0].name").value("Usuário professor"))
                .andExpect(jsonPath("$.conversations[0].memberIds[0].avatarUrl").value(url(Cenario.AVATAR)))
                .andExpect(jsonPath("$.conversations[0].lastMessage").value((Object) null));
        mvc.perform(get("/api/conversations").header(HttpHeaders.AUTHORIZATION, cenario.bearer(bia)))
                .andExpect(jsonPath("$.conversations[*]._id").isArray())
                .andExpect(jsonPath("$.conversations.length()").value(1));
    }

    @Test
    void contatosSaoOsSeguidoresMutuos() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario ana = cenario.usuario(Papel.ALUNO, faculdade, "Ana", false);
        Usuario mutua = cenario.usuario(Papel.PROFESSOR, faculdade, "Mútua", false);
        Usuario soSigo = cenario.usuario(Papel.ALUNO, faculdade, "Só Sigo", false);
        Usuario meSegue = cenario.usuario(Papel.ALUNO, faculdade, "Me Segue", false);
        mutuos(ana, mutua);
        seguir(ana, soSigo);
        seguir(meSegue, ana);

        mvc.perform(get("/api/conversations/eligible-contacts").header(HttpHeaders.AUTHORIZATION, cenario.bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"users":[{"_id":"%s","name":"Mútua","avatarUrl":"%s","role":"professor"}]}"""
                        .formatted(mutua.id(), url(Cenario.AVATAR)), true));
    }

    private void erro(ResultActions resposta, int status, String mensagem) throws Exception {
        resposta.andExpect(status().is(status)).andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private ResultActions iniciar(Usuario quem, String outro) throws Exception {
        return mvc.perform(post("/api/conversations").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + outro + "\"}"));
    }

    private void mutuos(Usuario a, Usuario b) throws Exception {
        seguir(a, b);
        seguir(b, a);
    }

    private void seguir(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
