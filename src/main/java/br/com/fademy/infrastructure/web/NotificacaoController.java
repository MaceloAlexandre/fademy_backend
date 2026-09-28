package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.LerNotificacaoUseCase;
import br.com.fademy.application.usecase.ListarNotificacoesUseCase;
import br.com.fademy.application.usecase.ListarNotificacoesUseCase.NotificacaoDetalhada;
import br.com.fademy.application.usecase.ListarNotificacoesUseCase.Resultado;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
public class NotificacaoController {

    private final ListarNotificacoesUseCase listarNotificacoes;
    private final LerNotificacaoUseCase lerNotificacao;
    private final EnderecoDasImagens imagens;

    public NotificacaoController(ListarNotificacoesUseCase listarNotificacoes, LerNotificacaoUseCase lerNotificacao,
                                 EnderecoDasImagens imagens) {
        this.listarNotificacoes = listarNotificacoes;
        this.lerNotificacao = lerNotificacao;
        this.imagens = imagens;
    }

    @GetMapping("/api/notifications")
    public NotificacoesResponse listar(Visualizador visualizador,
                                       @RequestParam(required = false) String cursor,
                                       @RequestParam(required = false) String limit) {
        Resultado resultado = listarNotificacoes.executar(visualizador, cursor, limit);
        return new NotificacoesResponse(
                resultado.pagina().itens().stream().map(d -> NotificacaoResponse.de(d, imagens)).toList(),
                resultado.pagina().temMais(), resultado.pagina().proximoCursor(), resultado.naoLidas());
    }

    @PatchMapping("/api/notifications/{id}/read")
    public LidaResponse marcarComoLida(Visualizador visualizador, @PathVariable String id) {
        lerNotificacao.marcarComoLida(visualizador, id);
        return new LidaResponse(true);
    }

    @DeleteMapping("/api/notifications/{id}")
    public ExcluidaResponse excluir(Visualizador visualizador, @PathVariable String id) {
        lerNotificacao.excluir(visualizador, id);
        return new ExcluidaResponse(true);
    }

    public record NotificacoesResponse(List<NotificacaoResponse> notifications, boolean hasMore, String nextCursor,
                                       long unreadCount) {
    }

    public record NotificacaoResponse(@JsonProperty("_id") String id, String userId, AtorResponse actorId,
                                      String type, String refId, String refModel, boolean read, Instant createdAt,
                                      @JsonInclude(JsonInclude.Include.NON_NULL) Optional<String> followStatus) {

        static NotificacaoResponse de(NotificacaoDetalhada d, EnderecoDasImagens imagens) {
            Notificacao n = d.notificacao();
            Optional<String> followStatus = n.tipo() == TipoNotificacao.FOLLOW_REQUEST
                    ? Optional.ofNullable(d.statusDoPedido()).map(s -> s.texto())
                    : null;
            return new NotificacaoResponse(n.id(), n.destinatarioId(), AtorResponse.de(d.ator(), imagens), n.tipo().texto(),
                    n.refId(), n.tipo().modeloReferencia(), n.lida(), n.criadaEm(), followStatus);
        }
    }

    public record AtorResponse(@JsonProperty("_id") String id, String name, String avatarUrl) {

        static AtorResponse de(Usuario u, EnderecoDasImagens imagens) {
            return u == null ? null : new AtorResponse(u.id(), u.nome(), imagens.url(u.avatarChave()));
        }
    }

    public record LidaResponse(boolean read) {
    }

    public record ExcluidaResponse(boolean deleted) {
    }
}
