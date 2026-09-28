package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FeedWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    CategoriaGateway categorias;

    @Test
    void soPostsDeQuemSigoComFollowAceitoComAutorECategorias() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario eu = cenario.usuario(Papel.ALUNO, faculdade, "Eu", false);
        Usuario sigo = cenario.usuario(Papel.PROFESSOR, faculdade, "Professora Sigo", false);
        Usuario pedi = cenario.usuario(Papel.ALUNO, faculdade, "Pedi", true);
        Usuario naoSigo = cenario.usuario(Papel.ALUNO, faculdade, "Não Sigo", false);
        seguir(eu, sigo);
        seguir(eu, pedi);
        Categoria aulas = categorias.listar().stream().filter(c -> c.slug().equals("aulas")).findFirst().orElseThrow();

        cenario.post(sigo, "primeiro", aulas.id());
        cenario.post(pedi, "de quem pedi");
        cenario.post(naoSigo, "de quem não sigo");
        cenario.post(eu, "meu");
        cenario.post(sigo, "segundo");

        feed(eu, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].text", contains("segundo", "primeiro")))
                .andExpect(jsonPath("$.posts[1].authorId._id").value(sigo.id()))
                .andExpect(jsonPath("$.posts[1].authorId.name").value("Professora Sigo"))
                .andExpect(jsonPath("$.posts[1].authorId.avatarUrl").value(url(Cenario.AVATAR)))
                .andExpect(jsonPath("$.posts[1].categoryIds[0]._id").value(aulas.id()))
                .andExpect(jsonPath("$.posts[1].categoryIds[0].slug").value("aulas"))
                .andExpect(jsonPath("$.posts[1].categoryIds[0].type").value("content"))
                .andExpect(jsonPath("$.posts[0].categoryIds", empty()))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.nextCursor").value((Object) null));
    }

    @Test
    void bloqueioEscondeOsPostsNosDoisSentidos() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario eu = cenario.usuario(Papel.ALUNO, faculdade, "Eu", false);
        Usuario bloqueei = cenario.usuario(Papel.ALUNO, faculdade, "Bloqueei", false);
        Usuario meBloqueou = cenario.usuario(Papel.ALUNO, faculdade, "Me Bloqueou", false);
        Usuario amiga = cenario.usuario(Papel.ALUNO, faculdade, "Amiga", false);
        for (Usuario u : new Usuario[]{bloqueei, meBloqueou, amiga}) {
            seguir(eu, u);
            cenario.post(u, "post de " + u.nome());
        }
        bloquear(eu, bloqueei);
        bloquear(meBloqueou, eu);

        feed(eu, "").andExpect(jsonPath("$.posts[*].text", contains("post de Amiga")));
    }

    @Test
    void paginacaoPorCursor() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario eu = cenario.usuario(Papel.ALUNO, faculdade, "Eu", false);
        Usuario autora = cenario.usuario(Papel.ALUNO, faculdade, "Autora", false);
        seguir(eu, autora);
        for (int i = 1; i <= 3; i++) {
            cenario.post(autora, "post " + i);
        }

        String primeira = feed(eu, "?limit=2")
                .andExpect(jsonPath("$.posts[*].text", contains("post 3", "post 2")))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn().getResponse().getContentAsString();

        feed(eu, "?limit=2&cursor=" + JsonPath.read(primeira, "$.nextCursor"))
                .andExpect(jsonPath("$.posts[*].text", contains("post 1")))
                .andExpect(jsonPath("$.hasMore").value(false));
        feed(eu, "?cursor=nao-e-cursor")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Invalid cursor\"}", true));
    }

    private ResultActions feed(Usuario quem, String parametros) throws Exception {
        return mvc.perform(get("/api/posts/feed" + parametros).header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private void seguir(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private void bloquear(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
