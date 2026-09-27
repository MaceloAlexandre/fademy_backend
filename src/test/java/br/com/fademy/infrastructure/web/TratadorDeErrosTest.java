package br.com.fademy.infrastructure.web;

import apoio.ControllerDeErrosDeTeste;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TratadorDeErrosTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControllerDeErrosDeTeste())
            .setControllerAdvice(new TratadorDeErros())
            .build();

    @Test
    void excecoesDoDominioViramStatusEMensagem() throws Exception {
        esperar("dados-invalidos", 400);
        esperar("nao-autenticado", 401);
        esperar("acesso-negado", 403);
        esperar("nao-encontrado", 404);
        esperar("conflito", 409);
    }

    @Test
    void excecaoInesperadaVira500SemDetalhes() throws Exception {
        mvc.perform(get("/teste/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"error\":\"Internal server error\"}", true));
    }

    @Test
    void jsonMalformadoVira400NoFormatoPadrao() throws Exception {
        mvc.perform(post("/teste/corpo").contentType(MediaType.APPLICATION_JSON).content("{nao é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void metodoNaoSuportadoVira405NoFormatoPadrao() throws Exception {
        mvc.perform(delete("/teste/corpo"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").isString());
    }

    private void esperar(String caso, int status) throws Exception {
        mvc.perform(get("/teste/" + caso))
                .andExpect(status().is(status))
                .andExpect(content().json("{\"error\":\"mensagem de " + caso + "\"}", true));
    }
}
