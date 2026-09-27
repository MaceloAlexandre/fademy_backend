package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.AutenticarUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AutenticarUseCase autenticar;

    public AuthController(AutenticarUseCase autenticar) {
        this.autenticar = autenticar;
    }

    /** SPEC RN07. */
    @PostMapping("/api/auth/login")
    public TokenResponse login(@RequestBody(required = false) LoginRequest corpo) {
        LoginRequest login = corpo != null ? corpo : new LoginRequest(null, null);
        return new TokenResponse(autenticar.executar(login.email(), login.password()));
    }

    public record LoginRequest(String email, String password) {
    }

    public record TokenResponse(String token) {
    }
}
