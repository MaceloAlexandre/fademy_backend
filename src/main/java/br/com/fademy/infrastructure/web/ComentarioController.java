package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.ComentarUseCase;
import br.com.fademy.application.usecase.ComentarioDetalhado;
import br.com.fademy.domain.conteudo.Comentario;
import br.com.fademy.domain.usuario.Visualizador;
import br.com.fademy.infrastructure.web.PostResponse.Autor;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class ComentarioController {

    private final ComentarUseCase comentar;

    public ComentarioController(ComentarUseCase comentar) {
        this.comentar = comentar;
    }

    /** SPEC RN32. */
    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioCriadoResponse comentar(Visualizador visualizador, @PathVariable String postId,
                                             @RequestBody(required = false) ComentarioRequest corpo) {
        String texto = corpo != null ? corpo.text() : null;
        return new ComentarioCriadoResponse(ComentarioResponse.de(comentar.comentar(visualizador, postId, texto)));
    }

    /** SPEC RN33. */
    @GetMapping("/api/posts/{postId}/comments")
    public ComentariosResponse listar(Visualizador visualizador, @PathVariable String postId) {
        return new ComentariosResponse(
                comentar.listar(visualizador, postId).stream().map(ComentarioResponse::de).toList());
    }

    public record ComentarioRequest(String text) {
    }

    public record ComentarioCriadoResponse(ComentarioResponse comment) {
    }

    public record ComentariosResponse(List<ComentarioResponse> comments) {
    }

    public record ComentarioResponse(@JsonProperty("_id") String id, String postId, Autor authorId,
                                     String institutionId, String text, Instant createdAt) {

        static ComentarioResponse de(ComentarioDetalhado d) {
            Comentario c = d.comentario();
            return new ComentarioResponse(c.id(), c.postId(), Autor.de(d.autor()), c.instituicaoId(), c.texto(),
                    c.criadoEm());
        }
    }
}
