package br.com.fademy.infrastructure.seguranca;

import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class JwtTokenGateway implements TokenGateway {

    private final Algorithm algoritmo;
    private final JWTVerifier verificador;
    private final Duration validade;

    public JwtTokenGateway(@Value("${fademy.jwt.segredo}") String segredo,
                           @Value("${fademy.jwt.validade}") String validade) {
        this.algoritmo = Algorithm.HMAC256(segredo);
        this.verificador = JWT.require(algoritmo).build();
        this.validade = Validade.ler(validade);
    }

    @Override
    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
                .withSubject(usuario.id())
                .withClaim("role", usuario.papel().texto())
                .withClaim("institutionId", usuario.instituicaoId())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plus(validade))
                .sign(algoritmo);
    }

    @Override
    public Visualizador validar(String token) {
        try {
            DecodedJWT jwt = verificador.verify(token);
            return new Visualizador(
                    jwt.getSubject(),
                    Papel.deTexto(jwt.getClaim("role").asString()),
                    jwt.getClaim("institutionId").asString());
        } catch (RuntimeException e) {
            throw new NaoAutenticadoException("Invalid or expired token");
        }
    }
}
