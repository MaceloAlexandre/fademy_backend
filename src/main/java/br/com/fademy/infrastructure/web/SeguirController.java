package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.DeixarDeSeguirUseCase;
import br.com.fademy.application.usecase.SeguirUsuarioUseCase;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.usuario.Visualizador;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeguirController {

    private final SeguirUsuarioUseCase seguirUsuario;
    private final DeixarDeSeguirUseCase deixarDeSeguir;

    public SeguirController(SeguirUsuarioUseCase seguirUsuario, DeixarDeSeguirUseCase deixarDeSeguir) {
        this.seguirUsuario = seguirUsuario;
        this.deixarDeSeguir = deixarDeSeguir;
    }

    @PostMapping("/api/users/{id}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public SeguirResponse seguir(Visualizador visualizador, @PathVariable String id) {
        Seguir seguir = seguirUsuario.executar(visualizador, id);
        return new SeguirResponse(seguir.status().texto(), seguir.id());
    }

    @DeleteMapping("/api/users/{id}/follow")
    public DeixouDeSeguirResponse deixarDeSeguir(Visualizador visualizador, @PathVariable String id) {
        deixarDeSeguir.executar(visualizador, id);
        return new DeixouDeSeguirResponse(true);
    }

    public record SeguirResponse(String status, String followId) {
    }

    public record DeixouDeSeguirResponse(boolean unfollowed) {
    }
}
