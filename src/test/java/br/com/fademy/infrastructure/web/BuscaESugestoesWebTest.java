package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BuscaESugestoesWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Test
    void buscaVaziaNaoConsulta() throws Exception {
        Usuario eu = cenario.usuario(Papel.ALUNO);

        buscar(eu, "").andExpect(status().isOk())
                .andExpect(content().json("{\"users\":[],\"total\":0,\"page\":1,\"limit\":20}", true));
        mvc.perform(get("/api/users/search").header(HttpHeaders.AUTHORIZATION, cenario.bearer(eu)))
                .andExpect(content().json("{\"users\":[],\"total\":0,\"page\":1}"));
    }

    @Test
    void buscaSemAcentoNemCaixaDentroDaInstituicao() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario eu = cenario.usuario(Papel.ALUNO, faculdade, "Marcos Eu", false);
        cenario.usuario(Papel.PROFESSOR, faculdade, "Márcia Lima", true);
        cenario.usuario(Papel.ALUNO, faculdade, "MARIO Souza", false);
        cenario.usuario(Papel.ALUNO, faculdade, "Ana Paula", false);
        Usuario bloqueadora = cenario.usuario(Papel.ALUNO, faculdade, "Marina Bloqueia", false);
        Usuario bloqueado = cenario.usuario(Papel.ALUNO, faculdade, "Maria Bloqueada", false);
        cenario.usuario(Papel.ALUNO, cenario.instituicaoNova(), "Marta de Outra", false);
        bloquear(bloqueadora, eu);
        bloquear(eu, bloqueado);

        buscar(eu, "mar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.users[*].name", contains("Márcia Lima", "MARIO Souza")))
                .andExpect(jsonPath("$.users[0]._id").isString())
                .andExpect(jsonPath("$.users[0].role").value("professor"))
                .andExpect(jsonPath("$.users[0].isPrivate").value(true))
                .andExpect(jsonPath("$.users[0].avatarUrl").value(url(Cenario.AVATAR)));
    }

    @Test
    void curingaDoLikeValeComoTexto() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario eu = cenario.usuario(Papel.ALUNO, faculdade, "Eu", false);
        cenario.usuario(Papel.ALUNO, faculdade, "Ana 100%", false);
        cenario.usuario(Papel.ALUNO, faculdade, "Bia 1000", false);

        buscar(eu, "0%").andExpect(jsonPath("$.users[*].name", contains("Ana 100%")));
    }

    @Test
    void paginacao() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario eu = cenario.usuario(Papel.ALUNO, faculdade, "Eu", false);
        for (String nome : new String[]{"Aluno A", "Aluno B", "Aluno C"}) {
            cenario.usuario(Papel.ALUNO, faculdade, nome, false);
        }

        mvc.perform(get("/api/users/search?q=aluno&page=2&limit=2").header(HttpHeaders.AUTHORIZATION, cenario.bearer(eu)))
                .andExpect(jsonPath("$.users[*].name", contains("Aluno C")))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.limit").value(2));
        mvc.perform(get("/api/users/search?q=aluno&limit=500").header(HttpHeaders.AUTHORIZATION, cenario.bearer(eu)))
                .andExpect(jsonPath("$.limit").value(50));
    }

    @Test
    void sugestoesEquipePrimeiroSemQuemJaSigoNemBloqueios() throws Exception {
        Instituicao faculdade = cenario.instituicaoNova();
        Usuario eu = cenario.usuario(Papel.ALUNO, faculdade, "Eu", false);
        cenario.usuario(Papel.ALUNO, faculdade, "Aluno Antigo", false);
        cenario.usuario(Papel.PROFESSOR, faculdade, "Professor", false);
        cenario.usuario(Papel.ALUNO, faculdade, "Aluno Novo", false);
        cenario.usuario(Papel.COORDENADOR, faculdade, "Coordenadora", false);
        Usuario jaSigo = cenario.usuario(Papel.ALUNO, faculdade, "Já Sigo", false);
        Usuario pedi = cenario.usuario(Papel.PROFESSOR, faculdade, "Pedi Pra Seguir", true);
        Usuario bloqueado = cenario.usuario(Papel.ALUNO, faculdade, "Bloqueado", false);
        cenario.usuario(Papel.PROFESSOR, cenario.instituicaoNova(), "De Outra", false);
        seguir(eu, jaSigo);
        seguir(eu, pedi);
        bloquear(eu, bloqueado);

        sugestoes(eu, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].name", contains("Coordenadora", "Professor", "Aluno Novo", "Aluno Antigo")));
        sugestoes(eu, "2").andExpect(jsonPath("$.users", hasSize(2)));
        sugestoes(eu, "99").andExpect(jsonPath("$.users[*].name",
                containsInAnyOrder("Coordenadora", "Professor", "Aluno Novo", "Aluno Antigo")));
    }

    private ResultActions buscar(Usuario quem, String q) throws Exception {
        return mvc.perform(get("/api/users/search").param("q", q).header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private ResultActions sugestoes(Usuario quem, String limite) throws Exception {
        var requisicao = get("/api/users/suggestions").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem));
        return mvc.perform(limite == null ? requisicao : requisicao.param("limit", limite));
    }

    private void seguir(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/follow").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }

    private void bloquear(Usuario quem, Usuario alvo) throws Exception {
        mvc.perform(post("/api/users/" + alvo.id() + "/block").header(HttpHeaders.AUTHORIZATION, cenario.bearer(quem)));
    }
}
