package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PublicarPostWebTest extends IntegracaoTest {

    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, 7, 7};

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    CategoriaGateway categorias;

    @Test
    void alunoPublicaComImagensECategorias() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        List<Categoria> todas = categorias.listar();

        String resposta = mvc.perform(post(ana)
                        .file(new MockMultipartFile("images", "a.jpg", "image/jpeg", FOTO))
                        .file(new MockMultipartFile("images", "b.png", "image/png", FOTO))
                        .param("text", "  Primeiro post  ")
                        .param("categoryIds", todas.get(0).id())
                        .param("categoryIds", todas.get(1).id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.post._id").isString())
                .andExpect(jsonPath("$.post.authorId").value(ana.id()))
                .andExpect(jsonPath("$.post.institutionId").value(ana.instituicaoId()))
                .andExpect(jsonPath("$.post.text").value("Primeiro post"))
                .andExpect(jsonPath("$.post.imageUrls", hasSize(2)))
                .andExpect(jsonPath("$.post.hasLink").value(false))
                .andExpect(jsonPath("$.post.linkUrl").doesNotExist())
                .andExpect(jsonPath("$.post.categoryIds", contains(todas.get(0).id(), todas.get(1).id())))
                .andExpect(jsonPath("$.post.commentCount").value(0))
                .andExpect(jsonPath("$.post.createdAt").isString())
                .andReturn().getResponse().getContentAsString();

        String url = JsonPath.read(resposta, "$.post.imageUrls[0]");
        assertThat(url).startsWith(url("posts/"));
        mvc.perform(get(url.substring(url.indexOf("/api/media/"))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FOTO));
    }

    @Test
    void categoriasSeparadasPorVirgula() throws Exception {
        List<Categoria> todas = categorias.listar();

        mvc.perform(post(cenario.usuario(Papel.ALUNO)).param("text", "oi")
                        .param("categoryIds", todas.get(2).id() + ", " + todas.get(3).id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.post.categoryIds", containsInAnyOrder(todas.get(2).id(), todas.get(3).id())));
    }

    @Test
    void categoriaInexistente() throws Exception {
        for (String id : new String[]{UUID.randomUUID().toString(), "nao-e-id"}) {
            erro(post(cenario.usuario(Papel.ALUNO)).param("text", "oi").param("categoryIds", id),
                    400, "Invalid category");
        }
    }

    @Test
    void textoObrigatorio() throws Exception {
        erro(post(cenario.usuario(Papel.ALUNO)).param("text", "   "), 400, "text is required");
        erro(post(cenario.usuario(Papel.ALUNO)), 400, "text is required");
    }

    @Test
    void linkSoParaAEquipe() throws Exception {
        erro(post(cenario.usuario(Papel.ALUNO)).param("text", "veja").param("linkUrl", "http://site"),
                403, "Insufficient permissions");

        mvc.perform(post(cenario.usuario(Papel.PROFESSOR)).param("text", "veja").param("linkUrl", " http://site "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.post.linkUrl").value("http://site"))
                .andExpect(jsonPath("$.post.hasLink").value(true));
    }

    @Test
    void imagensInvalidas() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);

        erro(post(ana).file(new MockMultipartFile("images", "a.gif", "image/gif", FOTO)).param("text", "oi"),
                400, "Invalid file type");

        MockMultipartHttpServletRequestBuilder cinco = post(ana);
        for (int i = 0; i < 5; i++) {
            cinco.file(new MockMultipartFile("images", i + ".jpg", "image/jpeg", FOTO));
        }
        erro(cinco.param("text", "oi"), 400, "File upload error");
    }

    private void erro(MockMultipartHttpServletRequestBuilder requisicao, int status, String mensagem) throws Exception {
        mvc.perform(requisicao)
                .andExpect(status().is(status))
                .andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private MockMultipartHttpServletRequestBuilder post(Usuario quem) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem));
    }
}
