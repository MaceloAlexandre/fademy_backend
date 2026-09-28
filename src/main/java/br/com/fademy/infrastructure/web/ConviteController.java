package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.ConsultarConviteUseCase;
import br.com.fademy.application.usecase.ConsultarConviteUseCase.ConsultaConvite;
import br.com.fademy.application.usecase.GerarConviteUseCase;
import br.com.fademy.application.usecase.RevogarConviteUseCase;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.usuario.Visualizador;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class ConviteController {

    private final GerarConviteUseCase gerarConvite;
    private final RevogarConviteUseCase revogarConvite;
    private final ConsultarConviteUseCase consultarConvite;

    public ConviteController(GerarConviteUseCase gerarConvite, RevogarConviteUseCase revogarConvite,
                             ConsultarConviteUseCase consultarConvite) {
        this.gerarConvite = gerarConvite;
        this.revogarConvite = revogarConvite;
        this.consultarConvite = consultarConvite;
    }

    @PostMapping("/api/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public ConviteGeradoResponse gerar(Visualizador visualizador,
                                       @RequestBody(required = false) GerarConviteRequest corpo) {
        GerarConviteRequest pedido = corpo != null ? corpo : new GerarConviteRequest(null, null, null);
        Convite convite = gerarConvite.executar(visualizador, pedido.roleTarget(), pedido.maxUses(), pedido.expiresAt());
        return new ConviteGeradoResponse(convite.token(), "/invite/" + convite.token());
    }

    @GetMapping("/api/invites/{token}")
    public ConsultaConviteResponse consultar(@PathVariable String token) {
        ConsultaConvite consulta = consultarConvite.executar(token);
        return new ConsultaConviteResponse(consulta.papelAlvo().texto(), consulta.nomeInstituicao(),
                consulta.vagasRestantes(), consulta.situacao().valido(), consulta.situacao().motivo());
    }

    @PostMapping("/api/invites/{token}/revoke")
    public ConviteRevogadoResponse revogar(Visualizador visualizador, @PathVariable String token) {
        revogarConvite.executar(visualizador, token);
        return new ConviteRevogadoResponse("Invite revoked", token);
    }

    public record GerarConviteRequest(String roleTarget, Integer maxUses, Instant expiresAt) {
    }

    public record ConviteGeradoResponse(String token, String link) {
    }

    public record ConviteRevogadoResponse(String message, String token) {
    }

    public record ConsultaConviteResponse(String roleTarget, String institutionName, int remainingSlots,
                                          boolean valid, String reason) {
    }
}
