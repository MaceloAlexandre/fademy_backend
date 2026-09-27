package br.com.fademy.domain.convite;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConviteTest {

    private static final Instant AGORA = Instant.parse("2026-09-27T12:00:00Z");

    private final Usuario professor = usuario(Papel.PROFESSOR);
    private final Usuario admin = usuario(Papel.ADMIN);

    @Test
    void geraConviteAtivoNaInstituicaoDoAutorValendoSeteDias() {
        Convite convite = Convite.gerar(professor, "aluno", 30, null, AGORA);

        assertThat(convite.papelAlvo()).isEqualTo(Papel.ALUNO);
        assertThat(convite.maxUsos()).isEqualTo(30);
        assertThat(convite.usos()).isZero();
        assertThat(convite.status()).isEqualTo(StatusConvite.ACTIVE);
        assertThat(convite.instituicaoId()).isEqualTo("inst");
        assertThat(convite.convidadoPorId()).isEqualTo("id-professor");
        assertThat(convite.expiraEm()).isEqualTo(AGORA.plus(Duration.ofDays(7)));
        assertThat(convite.token()).hasSize(36);
    }

    @Test
    void respeitaAValidadeInformada() {
        Instant data = AGORA.plus(Duration.ofDays(2));

        assertThat(Convite.gerar(professor, "aluno", 1, data, AGORA).expiraEm()).isEqualTo(data);
    }

    @Test
    void soAdminConvidaAdmin() {
        assertThatThrownBy(() -> Convite.gerar(professor, "admin", 1, null, AGORA))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Only admin can generate admin invites");
        assertThat(Convite.gerar(admin, "admin", 1, null, AGORA).papelAlvo()).isEqualTo(Papel.ADMIN);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL,    10,  roleTarget and maxUses are required",
            "aluno,   NULL, roleTarget and maxUses are required",
            "reitor,  10,  Invalid roleTarget",
            "aluno,   0,   maxUses must be at least 1",
            "aluno,   51,  maxUses exceeds cap of 50 for roleTarget aluno",
            "professor, 11, maxUses exceeds cap of 10 for roleTarget professor",
    }, nullValues = "NULL")
    void validacoesDaRN10(String papelAlvo, Integer maxUsos, String mensagem) {
        assertThatThrownBy(() -> Convite.gerar(professor, papelAlvo, maxUsos, null, AGORA))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(mensagem);
    }

    @Test
    void conviteDeAdminPorProfessorBarraAntesDoLimiteDeVagas() {
        assertThatThrownBy(() -> Convite.gerar(professor, "admin", 999, null, AGORA))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void revogar() {
        Convite convite = Convite.gerar(professor, "aluno", 1, null, AGORA);

        convite.revogar();

        assertThat(convite.status()).isEqualTo(StatusConvite.REVOKED);
    }

    private static Usuario usuario(Papel papel) {
        return papel.criarUsuario(new DadosUsuario(
                "id-" + papel.texto(), "inst", "Nome", "x@x.com", "hash", null, false, true, AGORA));
    }

    @Test
    void situacaoValida() {
        Convite convite = convite(StatusConvite.ACTIVE, 3, 10, AGORA.plusSeconds(60));

        assertThat(convite.situacao(AGORA)).isEqualTo(SituacaoConvite.VALIDO);
        assertThat(convite.situacao(AGORA).valido()).isTrue();
        assertThat(convite.situacao(AGORA).motivo()).isNull();
        assertThat(convite.vagasRestantes()).isEqualTo(7);
    }

    @ParameterizedTest
    @CsvSource({
            // status,  usos, max, expira em (s), situação
            "REVOKED,   0,    10,   60,  REVOGADO",
            "ACTIVE,    0,    10,  -60,  EXPIRADO",
            "ACTIVE,    10,   10,   60,  CHEIO",
            "EXPIRED,   10,   10,   60,  CHEIO",
            "REVOKED,   10,   10,  -60,  REVOGADO",
            "ACTIVE,    10,   10,  -60,  EXPIRADO",
    })
    void situacaoComPrecedencia(StatusConvite status, int usos, int max, long expiraEmSegundos,
                                SituacaoConvite esperada) {
        Convite convite = convite(status, usos, max, AGORA.plusSeconds(expiraEmSegundos));

        assertThat(convite.situacao(AGORA)).isEqualTo(esperada);
        assertThat(convite.situacao(AGORA).valido()).isFalse();
    }

    @Test
    void motivosDaApi() {
        assertThat(SituacaoConvite.REVOGADO.motivo()).isEqualTo("revoked");
        assertThat(SituacaoConvite.EXPIRADO.motivo()).isEqualTo("expired");
        assertThat(SituacaoConvite.CHEIO.motivo()).isEqualTo("full");
    }

    private static Convite convite(StatusConvite status, int usos, int max, Instant expiraEm) {
        return new Convite("id", "inst", "autor", Papel.ALUNO, "token", max, usos, status, expiraEm, AGORA);
    }
}
