package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.PublicarPostUseCase;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * A API reiniciada com outro S3_PUBLIC_URL (outro IP na apresentação) devolve as imagens que
 * já existiam com o endereço novo (#53).
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "fademy.s3.url-publica=http://outro-ip:8333/fademy")
class ImagensComOutroEnderecoTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    PublicarPostUseCase publicar;

    @Test
    void fotosEImagensSaemComOEnderecoDaExecucaoAtual() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Post post = publicar.executar(new Visualizador(ana.id(), ana.papel(), ana.instituicaoId()), "com foto", null,
                List.of(), List.of(new ArquivoEnviado(new byte[]{1}, "image/png")));

        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, cenario.bearer(ana)))
                .andExpect(jsonPath("$.avatarUrl").value("http://outro-ip:8333/fademy/" + Cenario.AVATAR));
        mvc.perform(get("/api/posts/" + post.id()).header(HttpHeaders.AUTHORIZATION, cenario.bearer(ana)))
                .andExpect(jsonPath("$.post.imageUrls[0]", startsWith("http://outro-ip:8333/fademy/posts/")))
                .andExpect(jsonPath("$.post.authorId.avatarUrl").value("http://outro-ip:8333/fademy/" + Cenario.AVATAR));
    }
}
