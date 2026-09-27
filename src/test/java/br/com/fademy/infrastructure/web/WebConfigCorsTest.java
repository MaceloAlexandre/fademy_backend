package br.com.fademy.infrastructure.web;

import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.usecase.VerificarSaudeUseCase;
import br.com.fademy.application.usecase.VerificarSaudeUseCase.Saude;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(SaudeController.class)
class WebConfigCorsTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VerificarSaudeUseCase verificarSaude;

    @MockitoBean
    TokenGateway tokens;

    @Test
    void origemDoFrontendEhLiberada() throws Exception {
        when(verificarSaude.executar()).thenReturn(new Saude(true, true));

        mvc.perform(get("/health").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void preflightLiberaPatchEDelete() throws Exception {
        mvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void outraOrigemNaoEhLiberada() throws Exception {
        when(verificarSaude.executar()).thenReturn(new Saude(true, true));

        mvc.perform(get("/health").header(HttpHeaders.ORIGIN, "http://site-qualquer.com"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
