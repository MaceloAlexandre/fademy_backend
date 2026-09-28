package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.midia.Imagem;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PerfilWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    UsuarioGateway usuarios;

    @Autowired
    ArmazenamentoGateway armazenamento;

    @Test
    void perfilPublicoCompletoComContagens() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.PROFESSOR);
        Usuario carla = cenario.usuario(Papel.ALUNO);
        seguir(ana, bia);
        seguir(carla, bia);
        seguir(bia, carla);

        perfil(ana, bia.id())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":"%s","name":"Usuário professor","avatarUrl":"%s","role":"professor",
                         "isPrivate":false,"followStatus":"accepted","isBlockedByMe":false,
                         "followersCount":2,"followingCount":1}""".formatted(bia.id(), url(Cenario.AVATAR)), true));
    }

    @Test
    void perfilPrivadoSemFollowAceitoNaoTemContagens() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.privado(Papel.ALUNO);
        seguir(ana, bia);

        perfil(ana, bia.id())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":"%s","name":"Usuário aluno","avatarUrl":"%s","role":"aluno",
                         "isPrivate":true,"followStatus":"pending","isBlockedByMe":false}"""
                        .formatted(bia.id(), url(Cenario.AVATAR)), true));
    }

    @Test
    void semFollowOStatusVemNulo() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);

        perfil(ana, cenario.usuario(Papel.ALUNO).id()).andExpect(jsonPath("$.followStatus").value((Object) null));
    }

    @Test
    void perfisInvisiveis() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bloqueadora = cenario.usuario(Papel.ALUNO);
        bloquear(bloqueadora, ana);

        for (String id : new String[]{bloqueadora.id(), cenario.usuario(Papel.ALUNO, cenario.instituicao("outra-faculdade")).id(),
                UUID.randomUUID().toString(), "id-invalido"}) {
            perfil(ana, id)
                    .andExpect(status().isNotFound())
                    .andExpect(content().json("{\"error\":\"User not found\"}", true));
        }
        perfil(bloqueadora, ana.id()).andExpect(status().isOk()).andExpect(jsonPath("$.isBlockedByMe").value(true));
    }

    @Test
    void meuPerfil() throws Exception {
        Usuario ana = cenario.privado(Papel.ALUNO);

        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, cenario.bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ana.id()))
                .andExpect(jsonPath("$.followersCount").value(0));
    }

    @Test
    void editarPrivacidade() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);

        editar(ana, "{\"isPrivate\":true}")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":"%s","name":"Usuário aluno","avatarUrl":"%s","role":"aluno","isPrivate":true}"""
                        .formatted(ana.id(), url(Cenario.AVATAR)), true));
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().privado()).isTrue();
    }

    @Test
    void trocarFoto() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        String chave = "avatars/" + UUID.randomUUID() + ".png";
        armazenamento.enviar(chave, new Imagem(new byte[]{1}, "image/png"));

        editar(ana, "{\"avatarUrl\":\"http://ip-antigo/qualquer\",\"avatarKey\":\"" + chave + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(url(chave)));
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().avatarChave()).isEqualTo(chave);
    }

    @Test
    void validacoesDaEdicao() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);

        erro(editar(ana, "{\"isPrivate\":\"sim\"}"), "isPrivate must be a boolean");
        erro(editar(ana, "{\"avatarUrl\":\"http://a\"}"), "avatarUrl and avatarKey must be strings");
        erro(editar(ana, "{\"avatarUrl\":\"http://a\",\"avatarKey\":\"avatars/nao.png\"}"),
                "Profile photo not found in storage — please upload a photo first");
        erro(editar(ana, "{\"nome\":\"outro\"}"), "No valid fields to update");
    }

    private void erro(ResultActions resposta, String mensagem) throws Exception {
        resposta.andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private ResultActions perfil(Usuario quem, String id) throws Exception {
        return mvc.perform(get("/api/users/" + id).header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private ResultActions editar(Usuario quem, String corpo) throws Exception {
        return mvc.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private void seguir(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private void bloquear(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
