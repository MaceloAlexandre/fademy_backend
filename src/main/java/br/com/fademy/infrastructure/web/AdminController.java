package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.VerPainelAdminUseCase;
import br.com.fademy.application.usecase.VerPainelAdminUseCase.Painel;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Visualizador;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class AdminController {

    private final VerPainelAdminUseCase verPainelAdmin;

    public AdminController(VerPainelAdminUseCase verPainelAdmin) {
        this.verPainelAdmin = verPainelAdmin;
    }

    /** SPEC RN44. */
    @GetMapping("/api/admin/overview")
    public PainelResponse painel(Visualizador visualizador) {
        Painel painel = verPainelAdmin.executar(visualizador);
        Instituicao i = painel.instituicao();
        return new PainelResponse(
                new InstituicaoResponse(i.id(), i.nome(), i.slug(), i.logoUrl(), i.ativa(), i.criadaEm()),
                painel.usuarios());
    }

    public record InstituicaoResponse(@JsonProperty("_id") String id, String name, String slug, String logoUrl,
                                      boolean active, Instant createdAt) {
    }

    public record PainelResponse(InstituicaoResponse institution, long userCount) {
    }
}
