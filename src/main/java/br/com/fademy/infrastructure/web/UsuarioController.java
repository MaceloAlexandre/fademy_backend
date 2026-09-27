package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.EditarPerfilUseCase;
import br.com.fademy.application.usecase.EditarPerfilUseCase.EdicaoDePerfil;
import br.com.fademy.application.usecase.VerPerfilUseCase;
import br.com.fademy.application.usecase.VerPerfilUseCase.Perfil;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UsuarioController {

    private final VerPerfilUseCase verPerfil;
    private final EditarPerfilUseCase editarPerfil;

    public UsuarioController(VerPerfilUseCase verPerfil, EditarPerfilUseCase editarPerfil) {
        this.verPerfil = verPerfil;
        this.editarPerfil = editarPerfil;
    }

    /** SPEC RN17. */
    @GetMapping("/api/users/me")
    public PerfilResponse meuPerfil(Visualizador visualizador) {
        return PerfilResponse.de(verPerfil.executar(visualizador, visualizador.id()));
    }

    /** SPEC RN17. */
    @GetMapping("/api/users/{id}")
    public PerfilResponse perfil(Visualizador visualizador, @PathVariable String id) {
        return PerfilResponse.de(verPerfil.executar(visualizador, id));
    }

    /** SPEC RN18. O corpo vai cru para o caso de uso, que valida os tipos. */
    @PatchMapping("/api/users/me")
    public PerfilEditadoResponse editar(Visualizador visualizador,
                                        @RequestBody(required = false) Map<String, Object> corpo) {
        Map<String, Object> c = corpo != null ? corpo : Map.of();
        Usuario eu = editarPerfil.executar(visualizador, new EdicaoDePerfil(
                c.containsKey("isPrivate"), c.get("isPrivate"),
                c.containsKey("avatarUrl") || c.containsKey("avatarKey"), c.get("avatarUrl"), c.get("avatarKey")));
        return new PerfilEditadoResponse(eu.id(), eu.nome(), eu.avatarUrl(), eu.papel().texto(), eu.privado());
    }

    public record PerfilResponse(String id, String name, String avatarUrl, String role, boolean isPrivate,
                                 String followStatus, boolean isBlockedByMe,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) Long followersCount,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) Long followingCount) {

        static PerfilResponse de(Perfil p) {
            Usuario u = p.usuario();
            return new PerfilResponse(u.id(), u.nome(), u.avatarUrl(), u.papel().texto(), u.privado(),
                    p.statusDoVisualizador() != null ? p.statusDoVisualizador().texto() : null,
                    p.bloqueadoPeloVisualizador(), p.seguidores(), p.seguindo());
        }
    }

    public record PerfilEditadoResponse(String id, String name, String avatarUrl, String role, boolean isPrivate) {
    }
}
