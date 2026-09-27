package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PainelAdminWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Test
    void adminVeSuaInstituicaoEOsUsuariosDela() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario admin = cenario.usuario(Papel.ADMIN, faculdade);
        cenario.usuario(Papel.ALUNO, faculdade);
        cenario.usuario(Papel.PROFESSOR, faculdade);
        cenario.usuario(Papel.ALUNO, cenario.instituicaoNova());

        painel(admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institution._id").value(faculdade.id()))
                .andExpect(jsonPath("$.institution.name").value(faculdade.nome()))
                .andExpect(jsonPath("$.institution.slug").value(faculdade.slug()))
                .andExpect(jsonPath("$.institution.active").value(true))
                .andExpect(jsonPath("$.institution.createdAt").isString())
                .andExpect(jsonPath("$.userCount").value(3));
    }

    @ParameterizedTest
    @EnumSource(value = Papel.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void soAdmin(Papel papel) throws Exception {
        painel(cenario.usuario(papel))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"Insufficient permissions\"}", true));
    }

    private ResultActions painel(Usuario quem) throws Exception {
        return mvc.perform(get("/api/admin/overview").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
