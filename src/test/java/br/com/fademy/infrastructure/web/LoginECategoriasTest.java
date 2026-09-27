package br.com.fademy.infrastructure.web;

import br.com.fademy.IntegracaoTest;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LoginECategoriasTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Test
    void loginDoAdminDevolveToken() throws Exception {
        login("{\"email\":\"admin@fademy.com\",\"password\":\"changeme\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void credenciaisErradasDao401() throws Exception {
        login("{\"email\":\"admin@fademy.com\",\"password\":\"errada\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Invalid credentials\"}", true));
        login("{\"email\":\"ninguem@fademy.com\",\"password\":\"changeme\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Invalid credentials\"}", true));
    }

    @Test
    void faltaCampoDa400() throws Exception {
        login("{\"email\":\"admin@fademy.com\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"email and password are required\"}", true));
        login("{}").andExpect(status().isBadRequest());
    }

    @Test
    void categoriasSemTokenDa401() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Authentication required\"}", true));
    }

    @Test
    void categoriasComTokenInvalidoDa401() throws Exception {
        String hs512 = JWT.create().withSubject("x").withClaim("role", "admin").withClaim("institutionId", "y")
                .sign(Algorithm.HMAC512("fademy-segredo-de-desenvolvimento-troque-em-producao"));
        String expirado = JWT.create().withSubject("x").withClaim("role", "admin").withClaim("institutionId", "y")
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256("fademy-segredo-de-desenvolvimento-troque-em-producao"));

        for (String token : new String[]{"lixo", hs512, expirado}) {
            mvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("{\"error\":\"Invalid or expired token\"}", true));
        }
    }

    @Test
    void categoriasComTokenValido() throws Exception {
        String resposta = login("{\"email\":\"admin@fademy.com\",\"password\":\"changeme\"}")
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(resposta, "$.token");

        mvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(10)))
                .andExpect(jsonPath("$.categories[?(@.slug == 'ciencia-da-computacao')].type").value("area"))
                .andExpect(jsonPath("$.categories[0]._id").isString())
                .andExpect(jsonPath("$.categories[0].name").isString());
    }

    private ResultActions login(String corpo) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
}
