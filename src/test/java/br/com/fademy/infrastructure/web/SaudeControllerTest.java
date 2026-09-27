package br.com.fademy.infrastructure.web;

import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.usecase.VerificarSaudeUseCase;
import br.com.fademy.application.usecase.VerificarSaudeUseCase.Saude;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaudeController.class)
class SaudeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VerificarSaudeUseCase verificarSaude;

    @MockitoBean
    TokenGateway tokens;

    @Test
    void bancosNoArRespondem200() throws Exception {
        when(verificarSaude.executar()).thenReturn(new Saude(true, true));

        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ok\",\"postgres\":\"up\",\"mongo\":\"up\"}", true));
    }

    @Test
    void mongoForaResponde503() throws Exception {
        when(verificarSaude.executar()).thenReturn(new Saude(true, false));

        mvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"degraded\",\"postgres\":\"up\",\"mongo\":\"down\"}", true));
    }

    @Test
    void postgresForaResponde503() throws Exception {
        when(verificarSaude.executar()).thenReturn(new Saude(false, true));

        mvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"degraded\",\"postgres\":\"down\",\"mongo\":\"up\"}", true));
    }
}
