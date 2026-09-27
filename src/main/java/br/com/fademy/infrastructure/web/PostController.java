package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.PublicarPostUseCase;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Visualizador;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
public class PostController {

    private final PublicarPostUseCase publicarPost;

    public PostController(PublicarPostUseCase publicarPost) {
        this.publicarPost = publicarPost;
    }

    /** SPEC RN28. */
    @PostMapping("/api/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostCriadoResponse publicar(Visualizador visualizador,
                                       @RequestParam(required = false) String text,
                                       @RequestParam(required = false) String linkUrl,
                                       @RequestParam(required = false) List<String> categoryIds,
                                       @RequestParam(name = "images", required = false) List<MultipartFile> images)
            throws IOException {
        Post post = publicarPost.executar(visualizador, text, linkUrl, categoryIds, arquivos(images));
        return new PostCriadoResponse(PostCriado.de(post));
    }

    static List<ArquivoEnviado> arquivos(List<MultipartFile> enviados) throws IOException {
        List<ArquivoEnviado> arquivos = new ArrayList<>();
        for (MultipartFile arquivo : enviados == null ? List.<MultipartFile>of() : enviados) {
            arquivos.add(new ArquivoEnviado(arquivo.getBytes(), arquivo.getContentType()));
        }
        return arquivos;
    }

    public record PostCriadoResponse(PostCriado post) {
    }

    /** Post recém-criado: autor e categorias como ids (SPEC 5.3). */
    public record PostCriado(@JsonProperty("_id") String id, String authorId, String institutionId, String text,
                             List<String> imageUrls, @JsonInclude(JsonInclude.Include.NON_NULL) String linkUrl,
                             boolean hasLink, List<String> categoryIds, int commentCount, Instant createdAt) {

        static PostCriado de(Post p) {
            return new PostCriado(p.id(), p.autorId(), p.instituicaoId(), p.texto(), p.imagens(), p.linkUrl(),
                    p.temLink(), p.categoriaIds(), p.comentarios(), p.criadoEm());
        }
    }
}
