package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.domain.conteudo.Post;
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

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class VerPostEPostsDoUsuarioWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Test
    void verPostDaInstituicao() throws Exception {
        Usuario autora = cenario.usuario(Papel.PROFESSOR);
        Post post = cenario.post(autora, "aula de hoje");

        ver(cenario.usuario(Papel.ALUNO), post.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post._id").value(post.id()))
                .andExpect(jsonPath("$.post.text").value("aula de hoje"))
                .andExpect(jsonPath("$.post.authorId._id").value(autora.id()))
                .andExpect(jsonPath("$.post.authorId.name").value("Usuário professor"));
    }

    @Test
    void postsInvisiveis() throws Exception {
        Usuario eu = cenario.usuario(Papel.ALUNO);
        Usuario meBloqueou = cenario.usuario(Papel.ALUNO);
        bloquear(meBloqueou, eu);
        Post deQuemMeBloqueou = cenario.post(meBloqueou, "invisível");
        Post deOutra = cenario.post(cenario.usuario(Papel.ALUNO, cenario.instituicaoNova()), "outra faculdade");

        for (String id : new String[]{deQuemMeBloqueou.id(), deOutra.id(), UUID.randomUUID().toString(), "id-invalido"}) {
            ver(eu, id).andExpect(status().isNotFound())
                    .andExpect(content().json("{\"error\":\"Post not found\"}", true));
        }
    }

    @Test
    void postsDoUsuarioPaginados() throws Exception {
        Usuario autora = cenario.usuario(Papel.ALUNO);
        for (int i = 1; i <= 3; i++) {
            cenario.post(autora, "post " + i);
        }
        Usuario leitor = cenario.usuario(Papel.ALUNO);

        String primeira = posts(leitor, autora.id(), "?limit=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].text", contains("post 3", "post 2")))
                .andExpect(jsonPath("$.posts[0].authorId._id").value(autora.id()))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn().getResponse().getContentAsString();
        posts(leitor, autora.id(), "?limit=2&cursor=" + JsonPath.read(primeira, "$.nextCursor"))
                .andExpect(jsonPath("$.posts[*].text", contains("post 1")))
                .andExpect(jsonPath("$.hasMore").value(false));
        posts(autora, "me", "").andExpect(jsonPath("$.posts[*].text", contains("post 3", "post 2", "post 1")));
        posts(leitor, autora.id(), "?cursor=nao-e-cursor").andExpect(status().isBadRequest());
    }

    @Test
    void perfilPrivadoSoParaDonoESeguidores() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario privada = cenario.usuario(Papel.ALUNO, faculdade, "Privada", true);
        cenario.post(privada, "só para seguidores");
        Usuario estranho = cenario.usuario(Papel.ALUNO, faculdade, "Estranho", false);
        Usuario pediu = cenario.usuario(Papel.ALUNO, faculdade, "Pediu", false);
        Usuario seguidor = cenario.usuario(Papel.ALUNO, faculdade, "Seguidor", false);
        seguir(pediu, privada);
        responder(privada, seguir(seguidor, privada), "accept");

        proibido(posts(estranho, privada.id(), ""));
        proibido(posts(pediu, privada.id(), ""));
        posts(seguidor, privada.id(), "").andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].text", contains("só para seguidores")));
        posts(privada, "me", "").andExpect(status().isOk());
    }

    @Test
    void quemMeBloqueouEscondeOsPosts() throws Exception {
        Usuario eu = cenario.usuario(Papel.ALUNO);
        Usuario meBloqueou = cenario.usuario(Papel.ALUNO);
        bloquear(meBloqueou, eu);

        proibido(posts(eu, meBloqueou.id(), ""));
    }

    @Test
    void autorInexistenteNaoTemPosts() throws Exception {
        posts(cenario.usuario(Papel.ALUNO), UUID.randomUUID().toString(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", empty()));
    }

    private void proibido(ResultActions resposta) throws Exception {
        resposta.andExpect(status().isForbidden()).andExpect(content().json("{\"error\":\"Forbidden\"}", true));
    }

    private ResultActions ver(Usuario quem, String id) throws Exception {
        return mvc.perform(get("/api/posts/" + id).header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private ResultActions posts(Usuario quem, String autor, String parametros) throws Exception {
        return mvc.perform(get("/api/users/" + autor + "/posts" + parametros)
                .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private String seguir(Usuario quem, Usuario alvo) throws Exception {
        return JsonPath.read(mvc.perform(post("/api/users/" + alvo.id() + "/follow")
                        .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem))).andReturn().getResponse().getContentAsString(),
                "$.followId");
    }

    private void responder(Usuario quem, String pedido, String acao) throws Exception {
        mvc.perform(patch("/api/follows/" + pedido + "/" + acao).header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private void bloquear(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
