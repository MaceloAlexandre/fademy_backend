package br.com.fademy.infrastructure.seguranca;

import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;

class JwtTokenGatewayTest {

    private static final String SEGREDO = "segredo-de-teste";
    private final JwtTokenGateway gateway = new JwtTokenGateway(SEGREDO, "7d");

    private final Usuario professor = Papel.PROFESSOR.criarUsuario(new DadosUsuario(
            "id-prof", "id-inst", "Prof", "prof@x.com", "hash", null, false, true, Instant.now()));

    @Test
    void tokenGeradoTemClaimsDoContratoEValeSeteDias() {
        DecodedJWT jwt = JWT.decode(gateway.gerar(professor));

        assertThat(jwt.getAlgorithm()).isEqualTo("HS256");
        assertThat(jwt.getSubject()).isEqualTo("id-prof");
        assertThat(jwt.getClaim("role").asString()).isEqualTo("professor");
        assertThat(jwt.getClaim("institutionId").asString()).isEqualTo("id-inst");
        assertThat(jwt.getExpiresAtAsInstant())
                .isCloseTo(Instant.now().plus(Duration.ofDays(7)), within(5, SECONDS));
    }

    @Test
    void tokenValidoViraVisualizador() {
        assertThat(gateway.validar(gateway.gerar(professor)))
                .isEqualTo(new Visualizador("id-prof", Papel.PROFESSOR, "id-inst"));
    }

    @Test
    void tokenExpiradoEhInvalido() {
        String expirado = JWT.create().withSubject("id-prof").withClaim("role", "professor")
                .withClaim("institutionId", "id-inst").withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SEGREDO));

        invalido(expirado);
    }

    @Test
    void outroAlgoritmoEhInvalido() {
        String hs512 = JWT.create().withSubject("id-prof").withClaim("role", "professor")
                .withClaim("institutionId", "id-inst").sign(Algorithm.HMAC512(SEGREDO));

        invalido(hs512);
    }

    @Test
    void outroSegredoEhInvalido() {
        invalido(new JwtTokenGateway("outro-segredo", "7d").gerar(professor));
    }

    @Test
    void lixoEhInvalido() {
        invalido("nao-e-um-jwt");
    }

    private void invalido(String token) {
        assertThatThrownBy(() -> gateway.validar(token))
                .isInstanceOf(NaoAutenticadoException.class)
                .hasMessage("Invalid or expired token");
    }
}
