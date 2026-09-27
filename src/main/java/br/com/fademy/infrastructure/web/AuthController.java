package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.AutenticarUseCase;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase.Cadastro;
import br.com.fademy.domain.usuario.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AutenticarUseCase autenticar;
    private final RegistrarViaConviteUseCase registrarViaConvite;

    public AuthController(AutenticarUseCase autenticar, RegistrarViaConviteUseCase registrarViaConvite) {
        this.autenticar = autenticar;
        this.registrarViaConvite = registrarViaConvite;
    }

    /** SPEC RN07. */
    @PostMapping("/api/auth/login")
    public TokenResponse login(@RequestBody(required = false) LoginRequest corpo) {
        LoginRequest login = corpo != null ? corpo : new LoginRequest(null, null);
        return new TokenResponse(autenticar.executar(login.email(), login.password()));
    }

    /** SPEC RN16. Devolve o usuário sem token: o frontend manda para o login. */
    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCadastradoResponse cadastrar(@RequestBody(required = false) CadastroRequest corpo) {
        CadastroRequest c = corpo != null ? corpo : new CadastroRequest(null, null, null, null, null, null);
        Usuario usuario = registrarViaConvite.executar(
                new Cadastro(c.token(), c.name(), c.email(), c.password(), c.avatarUrl(), c.avatarKey()));
        return new UsuarioCadastradoResponse(usuario.id(), usuario.nome(), usuario.email(), usuario.papel().texto(),
                usuario.instituicaoId(), usuario.avatarUrl());
    }

    public record LoginRequest(String email, String password) {
    }

    public record CadastroRequest(String token, String name, String email, String password, String avatarUrl,
                                  String avatarKey) {
    }

    public record UsuarioCadastradoResponse(String id, String name, String email, String role, String institutionId,
                                            String avatarUrl) {
    }

    public record TokenResponse(String token) {
    }
}
