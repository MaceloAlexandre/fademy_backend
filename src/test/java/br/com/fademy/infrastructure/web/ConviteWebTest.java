package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.convite.StatusConvite;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ConviteWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    ConviteGateway convites;

    @Test
    void professorGeraConviteAtivoValendoSeteDias() throws Exception {
        Usuario professor = cenario.usuario(Papel.PROFESSOR);

        String resposta = gerar(professor, "{\"roleTarget\":\"aluno\",\"maxUses\":30}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(resposta, "$.token");
        assertThat((String) JsonPath.read(resposta, "$.link")).isEqualTo("/invite/" + token);
        Convite convite = convites.buscarPorToken(token).orElseThrow();
        assertThat(convite.status()).isEqualTo(StatusConvite.ACTIVE);
        assertThat(convite.usos()).isZero();
        assertThat(convite.maxUsos()).isEqualTo(30);
        assertThat(convite.instituicaoId()).isEqualTo(professor.instituicaoId());
        assertThat(convite.convidadoPorId()).isEqualTo(professor.id());
        assertThat(convite.expiraEm()).isCloseTo(Instant.now().plus(Duration.ofDays(7)), within(10, SECONDS));
    }

    @Test
    void respeitaExpiresAt() throws Exception {
        String resposta = gerar(cenario.usuario(Papel.COORDENADOR),
                "{\"roleTarget\":\"aluno\",\"maxUses\":1,\"expiresAt\":\"2030-01-01T00:00:00Z\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(convites.buscarPorToken(JsonPath.read(resposta, "$.token")).orElseThrow().expiraEm())
                .isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
    }

    @Test
    void alunoRecebe403MesmoComCorpoInvalido() throws Exception {
        Usuario aluno = cenario.usuario(Papel.ALUNO);

        gerar(aluno, "{}")
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"Insufficient permissions\"}", true));
        mvc.perform(post("/api/invites/qualquer/revoke").header(HttpHeaders.AUTHORIZATION, cenario.bearer(aluno)))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"Insufficient permissions\"}", true));
    }

    @Test
    void conviteDeAdminSoPorAdmin() throws Exception {
        gerar(cenario.usuario(Papel.PROFESSOR), "{\"roleTarget\":\"admin\",\"maxUses\":1}")
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"Only admin can generate admin invites\"}", true));
        gerar(cenario.usuario(Papel.ADMIN), "{\"roleTarget\":\"admin\",\"maxUses\":1}")
                .andExpect(status().isCreated());
    }

    @Test
    void validacoesComMensagensDaRN10() throws Exception {
        Usuario professor = cenario.usuario(Papel.PROFESSOR);

        erro(professor, "{\"maxUses\":1}", "roleTarget and maxUses are required");
        erro(professor, "{\"roleTarget\":\"reitor\",\"maxUses\":1}", "Invalid roleTarget");
        erro(professor, "{\"roleTarget\":\"aluno\",\"maxUses\":0}", "maxUses must be at least 1");
        erro(professor, "{\"roleTarget\":\"aluno\",\"maxUses\":51}", "maxUses exceeds cap of 50 for roleTarget aluno");
        erro(professor, "{\"roleTarget\":\"professor\",\"maxUses\":11}",
                "maxUses exceeds cap of 10 for roleTarget professor");
    }

    @Test
    void revogaConviteDaInstituicao() throws Exception {
        Usuario professor = cenario.usuario(Papel.PROFESSOR);
        String token = JsonPath.read(gerar(professor, "{\"roleTarget\":\"aluno\",\"maxUses\":5}")
                .andReturn().getResponse().getContentAsString(), "$.token");

        mvc.perform(post("/api/invites/" + token + "/revoke")
                        .header(HttpHeaders.AUTHORIZATION, cenario.bearer(cenario.usuario(Papel.FUNCIONARIO))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"Invite revoked\",\"token\":\"" + token + "\"}", true));

        assertThat(convites.buscarPorToken(token).orElseThrow().status()).isEqualTo(StatusConvite.REVOKED);
    }

    @Test
    void conviteInexistenteOuDeOutraInstituicaoDa404() throws Exception {
        Usuario deOutra = cenario.usuario(Papel.PROFESSOR, cenario.instituicao("outra-faculdade"));
        String token = JsonPath.read(gerar(deOutra, "{\"roleTarget\":\"aluno\",\"maxUses\":5}")
                .andReturn().getResponse().getContentAsString(), "$.token");
        String bearer = cenario.bearer(cenario.usuario(Papel.PROFESSOR));

        for (String t : new String[]{token, "nao-existe"}) {
            mvc.perform(post("/api/invites/" + t + "/revoke").header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isNotFound())
                    .andExpect(content().json("{\"error\":\"Invite not found\"}", true));
        }
        assertThat(convites.buscarPorToken(token).orElseThrow().status()).isEqualTo(StatusConvite.ACTIVE);
    }

    private void erro(Usuario usuario, String corpo, String mensagem) throws Exception {
        gerar(usuario, corpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private ResultActions gerar(Usuario usuario, String corpo) throws Exception {
        return mvc.perform(post("/api/invites")
                .header(HttpHeaders.AUTHORIZATION, cenario.bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
