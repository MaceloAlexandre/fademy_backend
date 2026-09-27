package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.convite.StatusConvite;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ConsultaDeConviteWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    ConviteGateway convites;

    @Test
    void conviteValidoSemAutenticacao() throws Exception {
        String token = convite(StatusConvite.ACTIVE, 3, 10, Instant.now().plusSeconds(3600));

        mvc.perform(get("/api/invites/" + token))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"roleTarget":"aluno","institutionName":"Faculdade Católica da Paraíba",
                         "remainingSlots":7,"valid":true,"reason":null}""", true));
    }

    @Test
    void motivosQuandoNaoVale() throws Exception {
        Instant futuro = Instant.now().plusSeconds(3600);
        Instant passado = Instant.now().minusSeconds(3600);

        esperarMotivo(convite(StatusConvite.REVOKED, 0, 10, futuro), "revoked");
        esperarMotivo(convite(StatusConvite.ACTIVE, 0, 10, passado), "expired");
        esperarMotivo(convite(StatusConvite.EXPIRED, 10, 10, futuro), "full");
        esperarMotivo(convite(StatusConvite.REVOKED, 10, 10, passado), "revoked");
    }

    @Test
    void tokenInexistente() throws Exception {
        mvc.perform(get("/api/invites/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":\"Invite not found\"}", true));
    }

    private void esperarMotivo(String token, String motivo) throws Exception {
        mvc.perform(get("/api/invites/" + token))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"valid\":false,\"reason\":\"" + motivo + "\"}"));
    }

    private String convite(StatusConvite status, int usos, int max, Instant expiraEm) {
        Usuario professor = cenario.usuario(Papel.PROFESSOR);
        String token = UUID.randomUUID().toString();
        convites.criar(new Convite(null, professor.instituicaoId(), professor.id(), Papel.ALUNO, token,
                max, usos, status, expiraEm, Instant.now()));
        return token;
    }
}
