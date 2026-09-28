package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ComentarioWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    PostGateway posts;

    @Autowired
    MongoDatabase mongo;

    @Test
    void comentarSomaContadorENotificaOAutor() throws Exception {
        Usuario autora = cenario.usuario(Papel.PROFESSOR);
        Usuario aluna = cenario.usuario(Papel.ALUNO);
        Post post = cenario.post(autora, "aula");

        comentar(aluna, post.id(), "  ótima aula  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment._id").isString())
                .andExpect(jsonPath("$.comment.postId").value(post.id()))
                .andExpect(jsonPath("$.comment.authorId._id").value(aluna.id()))
                .andExpect(jsonPath("$.comment.authorId.name").value("Usuário aluno"))
                .andExpect(jsonPath("$.comment.authorId.avatarUrl").value(url(Cenario.AVATAR)))
                .andExpect(jsonPath("$.comment.institutionId").value(aluna.instituicaoId()))
                .andExpect(jsonPath("$.comment.text").value("ótima aula"))
                .andExpect(jsonPath("$.comment.createdAt").isString());

        assertThat(posts.buscarPorId(post.id()).orElseThrow().comentarios()).isEqualTo(1);
        assertThat(mongo.getCollection("notificacoes").countDocuments(Filters.and(
                Filters.eq("tipo", "post_comment"), Filters.eq("destinatarioId", autora.id()),
                Filters.eq("atorId", aluna.id()), Filters.eq("refId", post.id())))).isEqualTo(1);
    }

    @Test
    void comentarNoProprioPostNaoNotifica() throws Exception {
        Usuario autora = cenario.usuario(Papel.PROFESSOR);
        Post post = cenario.post(autora, "aula");

        comentar(autora, post.id(), "complemento").andExpect(status().isCreated());

        assertThat(mongo.getCollection("notificacoes").countDocuments(Filters.and(
                Filters.eq("tipo", "post_comment"), Filters.eq("refId", post.id())))).isZero();
    }

    @Test
    void validacoes() throws Exception {
        Usuario eu = cenario.usuario(Papel.ALUNO);
        Usuario meBloqueou = cenario.usuario(Papel.ALUNO);
        mvc.perform(post("/api/users/" + eu.id() + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(meBloqueou)));
        Post post = cenario.post(cenario.usuario(Papel.ALUNO), "post");

        erro(comentar(eu, post.id(), "   "), 400, "text is required");
        for (String id : new String[]{cenario.post(meBloqueou, "invisível").id(),
                cenario.post(cenario.usuario(Papel.ALUNO, cenario.instituicaoNova()), "outra").id(),
                UUID.randomUUID().toString(), "id-invalido"}) {
            erro(comentar(eu, id, "oi"), 404, "Post not found");
            erro(listar(eu, id), 404, "Post not found");
        }
    }

    @Test
    void listarDoMaisAntigoParaOMaisNovo() throws Exception {
        Post post = cenario.post(cenario.usuario(Papel.PROFESSOR), "aula");
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        comentar(ana, post.id(), "primeiro");
        comentar(bia, post.id(), "segundo");

        listar(ana, post.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[*].text", contains("primeiro", "segundo")))
                .andExpect(jsonPath("$.comments[*].authorId._id", contains(ana.id(), bia.id())));
    }

    private void erro(ResultActions resposta, int status, String mensagem) throws Exception {
        resposta.andExpect(status().is(status)).andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private ResultActions comentar(Usuario quem, String postId, String texto) throws Exception {
        return mvc.perform(post("/api/posts/" + postId + "/comments").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"" + texto + "\"}"));
    }

    private ResultActions listar(Usuario quem, String postId) throws Exception {
        return mvc.perform(get("/api/posts/" + postId + "/comments").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
