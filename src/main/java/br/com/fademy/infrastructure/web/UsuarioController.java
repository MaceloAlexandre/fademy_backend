package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.BuscarUsuariosUseCase;
import br.com.fademy.application.usecase.BuscarUsuariosUseCase.Resultado;
import br.com.fademy.application.usecase.EditarPerfilUseCase;
import br.com.fademy.application.usecase.EditarPerfilUseCase.EdicaoDePerfil;
import br.com.fademy.application.usecase.SugerirUsuariosUseCase;
import br.com.fademy.application.usecase.VerPerfilUseCase;
import br.com.fademy.application.usecase.VerPerfilUseCase.Perfil;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class UsuarioController {

    private final VerPerfilUseCase verPerfil;
    private final EditarPerfilUseCase editarPerfil;
    private final BuscarUsuariosUseCase buscarUsuarios;
    private final SugerirUsuariosUseCase sugerirUsuarios;
    private final EnderecoDasImagens imagens;

    public UsuarioController(VerPerfilUseCase verPerfil, EditarPerfilUseCase editarPerfil,
                             BuscarUsuariosUseCase buscarUsuarios, SugerirUsuariosUseCase sugerirUsuarios,
                             EnderecoDasImagens imagens) {
        this.verPerfil = verPerfil;
        this.editarPerfil = editarPerfil;
        this.buscarUsuarios = buscarUsuarios;
        this.sugerirUsuarios = sugerirUsuarios;
        this.imagens = imagens;
    }

    @GetMapping("/api/users/search")
    public BuscaResponse buscar(Visualizador visualizador,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String page,
                                @RequestParam(required = false) String limit) {
        Resultado resultado = buscarUsuarios.executar(visualizador, q, page, limit);
        return new BuscaResponse(resultado.usuarios().stream().map(u -> UsuarioResumoResponse.de(u, imagens)).toList(),
                resultado.total(), resultado.pagina(), resultado.limite());
    }

    @GetMapping("/api/users/suggestions")
    public SugestoesResponse sugerir(Visualizador visualizador, @RequestParam(required = false) String limit) {
        return new SugestoesResponse(
                sugerirUsuarios.executar(visualizador, limit).stream().map(u -> UsuarioResumoResponse.de(u, imagens))
                        .toList());
    }

    @GetMapping("/api/users/me")
    public PerfilResponse meuPerfil(Visualizador visualizador) {
        return PerfilResponse.de(verPerfil.executar(visualizador, visualizador.id()), imagens);
    }

    @GetMapping("/api/users/{id}")
    public PerfilResponse perfil(Visualizador visualizador, @PathVariable String id) {
        return PerfilResponse.de(verPerfil.executar(visualizador, id), imagens);
    }

    @PatchMapping("/api/users/me")
    public PerfilEditadoResponse editar(Visualizador visualizador,
                                        @RequestBody(required = false) Map<String, Object> corpo) {
        Map<String, Object> c = corpo != null ? corpo : Map.of();
        Usuario eu = editarPerfil.executar(visualizador, new EdicaoDePerfil(
                c.containsKey("isPrivate"), c.get("isPrivate"),
                c.containsKey("avatarUrl") || c.containsKey("avatarKey"), c.get("avatarUrl"), c.get("avatarKey")));
        return new PerfilEditadoResponse(eu.id(), eu.nome(), imagens.url(eu.avatarChave()), eu.papel().texto(),
                eu.privado());
    }

    public record PerfilResponse(String id, String name, String avatarUrl, String role, boolean isPrivate,
                                 String followStatus, boolean isBlockedByMe,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) Long followersCount,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) Long followingCount) {

        static PerfilResponse de(Perfil p, EnderecoDasImagens imagens) {
            Usuario u = p.usuario();
            return new PerfilResponse(u.id(), u.nome(), imagens.url(u.avatarChave()), u.papel().texto(), u.privado(),
                    p.statusDoVisualizador() != null ? p.statusDoVisualizador().texto() : null,
                    p.bloqueadoPeloVisualizador(), p.seguidores(), p.seguindo());
        }
    }

    public record UsuarioResumoResponse(@JsonProperty("_id") String id, String name, String avatarUrl, String role,
                                        boolean isPrivate) {

        static UsuarioResumoResponse de(Usuario u, EnderecoDasImagens imagens) {
            return new UsuarioResumoResponse(u.id(), u.nome(), imagens.url(u.avatarChave()), u.papel().texto(),
                    u.privado());
        }
    }

    public record BuscaResponse(List<UsuarioResumoResponse> users, long total, int page, int limit) {
    }

    public record SugestoesResponse(List<UsuarioResumoResponse> users) {
    }

    public record PerfilEditadoResponse(String id, String name, String avatarUrl, String role, boolean isPrivate) {
    }
}
