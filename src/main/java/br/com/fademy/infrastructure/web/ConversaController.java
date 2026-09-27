package br.com.fademy.infrastructure.web;

import br.com.fademy.application.gateway.ConversaGateway.Resultado;
import br.com.fademy.application.usecase.IniciarConversaUseCase;
import br.com.fademy.application.usecase.ListarConversasUseCase;
import br.com.fademy.application.usecase.ListarConversasUseCase.ConversaDetalhada;
import br.com.fademy.domain.conversa.Conversa;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import br.com.fademy.infrastructure.web.PostResponse.Autor;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class ConversaController {

    private final IniciarConversaUseCase iniciarConversa;
    private final ListarConversasUseCase listarConversas;

    public ConversaController(IniciarConversaUseCase iniciarConversa, ListarConversasUseCase listarConversas) {
        this.iniciarConversa = iniciarConversa;
        this.listarConversas = listarConversas;
    }

    /** SPEC RN38: 201 quando cria, 200 quando a conversa já existia. */
    @PostMapping("/api/conversations")
    public ResponseEntity<ConversaCriadaResponse> iniciar(Visualizador visualizador,
                                                          @RequestBody(required = false) IniciarRequest corpo) {
        Resultado resultado = iniciarConversa.executar(visualizador, corpo != null ? corpo.userId() : null);
        Conversa c = resultado.conversa();
        return ResponseEntity.status(resultado.criada() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(new ConversaCriadaResponse(new ConversaResponse<>(
                        c.id(), c.membros(), false, c.criadaPorId(), c.criadaEm(), c.atualizadaEm(), null)));
    }

    /** SPEC RN39. */
    @GetMapping("/api/conversations")
    public ConversasResponse listar(Visualizador visualizador) {
        return new ConversasResponse(
                listarConversas.conversas(visualizador).stream().map(ConversaController::de).toList());
    }

    /** SPEC RN40. */
    @GetMapping("/api/conversations/eligible-contacts")
    public ContatosResponse contatos(Visualizador visualizador) {
        return new ContatosResponse(listarConversas.contatos(visualizador).stream().map(ContatoResponse::de).toList());
    }

    private static ConversaResponse<Autor> de(ConversaDetalhada d) {
        Conversa c = d.conversa();
        return new ConversaResponse<>(c.id(), d.membros().stream().map(Autor::de).toList(), false, c.criadaPorId(),
                c.criadaEm(), c.atualizadaEm(),
                d.ultimaMensagem() != null ? MensagemResponse.de(d.ultimaMensagem()) : null);
    }

    public record IniciarRequest(String userId) {
    }

    /** Membros como ids (ao criar) ou populados (na lista), SPEC 5.3. */
    public record ConversaResponse<M>(@JsonProperty("_id") String id, List<M> memberIds, boolean isGroup,
                                      String createdById, Instant createdAt, Instant updatedAt,
                                      MensagemResponse lastMessage) {
    }

    public record ConversaCriadaResponse(ConversaResponse<String> conversation) {
    }

    public record ConversasResponse(List<ConversaResponse<Autor>> conversations) {
    }

    public record ContatoResponse(@JsonProperty("_id") String id, String name, String avatarUrl, String role) {

        static ContatoResponse de(Usuario u) {
            return new ContatoResponse(u.id(), u.nome(), u.avatarUrl(), u.papel().texto());
        }
    }

    public record ContatosResponse(List<ContatoResponse> users) {
    }
}
