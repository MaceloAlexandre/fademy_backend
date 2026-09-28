package br.com.fademy.infrastructure.armazenamento;

import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.ArmazenamentoGateway;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UploadDeAvatarTest extends IntegracaoTest {

    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

    @Autowired
    MockMvc mvc;

    @Autowired
    ArmazenamentoGateway armazenamento;

    @Test
    void uploadSemTokenDevolveChaveEUrlPublica() throws Exception {
        String resposta = mvc.perform(multipart("/api/media/upload")
                        .file(new MockMultipartFile("file", "minha-foto.jpg", "image/jpeg", FOTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String chave = JsonPath.read(resposta, "$.key");
        String url = JsonPath.read(resposta, "$.objectUrl");
        assertThat(chave).matches("avatars/[0-9a-f-]{36}\\.jpg").doesNotContain("minha-foto");
        assertThat(url).isEqualTo(url(chave));

        mvc.perform(get("/api/media/" + chave))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(FOTO));

        assertThat(armazenamento.existe(chave)).isTrue();
        assertThat(armazenamento.existe("avatars/nao-existe.jpg")).isFalse();
    }

    @Test
    void imagemInexistenteDa404() throws Exception {
        mvc.perform(get("/api/media/avatars/nao-existe.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":\"Image not found\"}", true));
    }

    @Test
    void semArquivo() throws Exception {
        mvc.perform(multipart("/api/media/upload"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"file is required\"}", true));
    }

    @Test
    void tipoNaoAceito() throws Exception {
        mvc.perform(multipart("/api/media/upload").file(new MockMultipartFile("file", "a.gif", "image/gif", FOTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"file must be image/jpeg, image/png, or image/webp\"}", true));
    }
}
