package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.BloquearUsuarioUseCase;
import br.com.fademy.application.usecase.DesbloquearUsuarioUseCase;
import br.com.fademy.domain.usuario.Visualizador;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BloqueioController {

    private final BloquearUsuarioUseCase bloquearUsuario;
    private final DesbloquearUsuarioUseCase desbloquearUsuario;

    public BloqueioController(BloquearUsuarioUseCase bloquearUsuario, DesbloquearUsuarioUseCase desbloquearUsuario) {
        this.bloquearUsuario = bloquearUsuario;
        this.desbloquearUsuario = desbloquearUsuario;
    }

    @PostMapping("/api/users/{id}/block")
    public BloqueadoResponse bloquear(Visualizador visualizador, @PathVariable String id) {
        bloquearUsuario.executar(visualizador, id);
        return new BloqueadoResponse(true);
    }

    @DeleteMapping("/api/users/{id}/block")
    public DesbloqueadoResponse desbloquear(Visualizador visualizador, @PathVariable String id) {
        desbloquearUsuario.executar(visualizador, id);
        return new DesbloqueadoResponse(true);
    }

    public record BloqueadoResponse(boolean blocked) {
    }

    public record DesbloqueadoResponse(boolean unblocked) {
    }
}
