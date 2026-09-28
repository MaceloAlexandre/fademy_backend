package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Visualizador;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class MensagemController {

    private final MensagensUseCase mensagens;
    private final EnderecoDasImagens imagens;

    public MensagemController(MensagensUseCase mensagens, EnderecoDasImagens imagens) {
        this.mensagens = mensagens;
        this.imagens = imagens;
    }

    @PostMapping("/api/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemCriadaResponse enviar(Visualizador visualizador, @PathVariable String id,
                                         @RequestParam(required = false) String text,
                                         @RequestParam(name = "images", required = false) List<MultipartFile> images)
            throws IOException {
        Mensagem mensagem = mensagens.enviar(visualizador, id, text, PostController.arquivos(images));
        return new MensagemCriadaResponse(MensagemResponse.de(mensagem, imagens));
    }

    @GetMapping("/api/conversations/{id}/messages")
    public PaginaDeMensagens listar(Visualizador visualizador, @PathVariable String id,
                                    @RequestParam(required = false) String cursor,
                                    @RequestParam(required = false) String limit) {
        Pagina<Mensagem> pagina = mensagens.listar(visualizador, id, cursor, limit);
        return new PaginaDeMensagens(pagina.itens().stream().map(m -> MensagemResponse.de(m, imagens)).toList(),
                pagina.temMais(),
                pagina.proximoCursor());
    }

    public record MensagemCriadaResponse(MensagemResponse message) {
    }

    public record PaginaDeMensagens(List<MensagemResponse> messages, boolean hasMore, String nextCursor) {
    }
}
