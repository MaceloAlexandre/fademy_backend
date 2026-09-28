package br.com.fademy.infrastructure.web;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase.Cadastro;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.convite.StatusConvite;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.midia.Imagem;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CadastroWebTest extends IntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Cenario cenario;

    @Autowired
    ConviteGateway convites;

    @Autowired
    ArmazenamentoGateway armazenamento;

    @Autowired
    RegistrarViaConviteUseCase registrarViaConvite;

    @Autowired
    MongoDatabase mongo;

    private Usuario professor;
    private String avatarKey;

    @BeforeEach
    void preparar() {
        professor = cenario.usuario(Papel.PROFESSOR);
        avatarKey = "avatars/" + UUID.randomUUID() + ".jpg";
        armazenamento.enviar(avatarKey, new Imagem(new byte[]{1, 2, 3}, "image/jpeg"));
    }

    @Test
    void cadastroCriaAlunoSemTokenEOAlunoConsegueEntrar() throws Exception {
        String token = convite(StatusConvite.ACTIVE, 0, 10, Instant.now().plusSeconds(3600));
        String email = "ana-" + UUID.randomUUID() + "@x.com";

        cadastrar(corpo(token, email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("aluno"))
                .andExpect(jsonPath("$.institutionId").value(professor.instituicaoId()))
                .andExpect(jsonPath("$.avatarUrl").value(url(avatarKey)))
                .andExpect(jsonPath("$.token").doesNotExist());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"segredo123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void validacoesNaOrdemDaRN16() throws Exception {
        Instant futuro = Instant.now().plusSeconds(3600);
        String valido = convite(StatusConvite.ACTIVE, 0, 10, futuro);

        erro(400, "{\"token\":\"" + valido + "\"}", "token, name, email, password, avatarUrl, and avatarKey are required");
        erro(400, corpo("nao-existe", "a@x.com"), "Invalid invite token");
        erro(400, corpo(convite(StatusConvite.REVOKED, 0, 10, futuro), "a@x.com"), "Invite is no longer valid");
        erro(400, corpo(convite(StatusConvite.ACTIVE, 0, 10, Instant.now().minusSeconds(60)), "a@x.com"),
                "Invite has expired");
        erro(400, corpo(convite(StatusConvite.ACTIVE, 10, 10, futuro), "a@x.com"), "Invite has no remaining slots");
        erro(400, corpo(valido, "a@x.com").replace(avatarKey, "avatars/nao-existe.jpg"),
                "Profile photo not found in storage — please upload a photo first");
        erro(409, corpo(valido, professor.email().toUpperCase()), "Este e-mail já está em uso");
    }

    @Test
    void camposExtrasNaoMudamPapelNemInstituicao() throws Exception {
        String token = convite(StatusConvite.ACTIVE, 0, 10, Instant.now().plusSeconds(3600));
        String corpo = corpo(token, "extra-" + UUID.randomUUID() + "@x.com")
                .replace("{", "{\"role\":\"admin\",\"institutionId\":\"" + UUID.randomUUID() + "\",");

        cadastrar(corpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("aluno"))
                .andExpect(jsonPath("$.institutionId").value(professor.instituicaoId()));
    }

    @Test
    void ultimaVagaExpiraOConvite() throws Exception {
        String token = convite(StatusConvite.ACTIVE, 0, 2, Instant.now().plusSeconds(3600));

        cadastrar(corpo(token, "um-" + UUID.randomUUID() + "@x.com")).andExpect(status().isCreated());
        Convite depoisDoPrimeiro = convites.buscarPorToken(token).orElseThrow();
        assertThat(depoisDoPrimeiro.usos()).isEqualTo(1);
        assertThat(depoisDoPrimeiro.status()).isEqualTo(StatusConvite.ACTIVE);

        cadastrar(corpo(token, "dois-" + UUID.randomUUID() + "@x.com")).andExpect(status().isCreated());
        Convite depoisDoSegundo = convites.buscarPorToken(token).orElseThrow();
        assertThat(depoisDoSegundo.usos()).isEqualTo(2);
        assertThat(depoisDoSegundo.status()).isEqualTo(StatusConvite.EXPIRED);
    }

    @Test
    void doisCadastrosSimultaneosNaUltimaVaga() throws Exception {
        String token = convite(StatusConvite.ACTIVE, 0, 1, Instant.now().plusSeconds(3600));
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        List<Future<String>> resultados = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String email = "corrida-" + UUID.randomUUID() + "@x.com";
            Callable<String> cadastro = () -> {
                largada.await();
                try {
                    registrarViaConvite.executar(new Cadastro(token, "Ana", email, "segredo123", "http://a", avatarKey));
                    return "criado";
                } catch (DadosInvalidosException e) {
                    return e.getMessage();
                }
            };
            resultados.add(threads.submit(cadastro));
        }
        largada.countDown();

        List<String> respostas = new ArrayList<>();
        for (Future<String> r : resultados) {
            respostas.add(r.get());
        }
        threads.shutdown();

        assertThat(respostas).containsExactlyInAnyOrder("criado", "Invite has no remaining slots");
        assertThat(convites.buscarPorToken(token).orElseThrow().usos()).isEqualTo(1);
    }

    @Test
    void emailRepetidoNaoConsomeVaga() throws Exception {
        String token = convite(StatusConvite.ACTIVE, 0, 10, Instant.now().plusSeconds(3600));

        cadastrar(corpo(token, professor.email())).andExpect(status().isConflict());

        assertThat(convites.buscarPorToken(token).orElseThrow().usos()).isZero();
    }

    @Test
    void autorDoConviteRecebeNotificacaoNoMongo() throws Exception {
        String token = convite(StatusConvite.ACTIVE, 0, 10, Instant.now().plusSeconds(3600));

        String resposta = cadastrar(corpo(token, "notif-" + UUID.randomUUID() + "@x.com"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String novoId = JsonPath.read(resposta, "$.id");

        Document notificacao = mongo.getCollection("notificacoes")
                .find(Filters.and(Filters.eq("destinatarioId", professor.id()), Filters.eq("atorId", novoId)))
                .first();
        assertThat(notificacao).isNotNull();
        assertThat(notificacao.getString("tipo")).isEqualTo("invite_received");
        assertThat(notificacao.getString("refId")).isEqualTo(novoId);
        assertThat(notificacao.getString("refModelo")).isEqualTo("User");
        assertThat(notificacao.getBoolean("lida")).isFalse();
    }

    private void erro(int status, String corpo, String mensagem) throws Exception {
        cadastrar(corpo)
                .andExpect(status().is(status))
                .andExpect(content().json("{\"error\":\"" + mensagem + "\"}", true));
    }

    private String corpo(String token, String email) {
        return """
                {"token":"%s","name":"Ana","email":"%s","password":"segredo123",
                 "avatarUrl":"http://avatar/ana.jpg","avatarKey":"%s"}""".formatted(token, email, avatarKey);
    }

    private ResultActions cadastrar(String corpo) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String convite(StatusConvite status, int usos, int max, Instant expiraEm) {
        String token = UUID.randomUUID().toString();
        convites.criar(new Convite(null, professor.instituicaoId(), professor.id(), Papel.ALUNO, token,
                max, usos, status, expiraEm, Instant.now()));
        return token;
    }
}
