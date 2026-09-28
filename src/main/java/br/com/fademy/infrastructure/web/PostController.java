package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.ListarPostsDoUsuarioUseCase;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.application.usecase.PostDetalhado;
import br.com.fademy.application.usecase.PublicarPostUseCase;
import br.com.fademy.application.usecase.VerFeedUseCase;
import br.com.fademy.application.usecase.VerPostUseCase;
import br.com.fademy.infrastructure.web.PostResponse.PaginaDePosts;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Visualizador;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final VerFeedUseCase verFeed;
    private final VerPostUseCase verPost;
    private final ListarPostsDoUsuarioUseCase listarPostsDoUsuario;
    private final EnderecoDasImagens imagens;

    public PostController(PublicarPostUseCase publicarPost, VerFeedUseCase verFeed, VerPostUseCase verPost,
                          ListarPostsDoUsuarioUseCase listarPostsDoUsuario, EnderecoDasImagens imagens) {
        this.publicarPost = publicarPost;
        this.verFeed = verFeed;
        this.verPost = verPost;
        this.listarPostsDoUsuario = listarPostsDoUsuario;
        this.imagens = imagens;
    }

    /** SPEC RN30. */
    @GetMapping("/api/posts/{id}")
    public PostUnicoResponse ver(Visualizador visualizador, @PathVariable String id) {
        return new PostUnicoResponse(PostResponse.de(verPost.executar(visualizador, id), imagens));
    }

    /** SPEC RN31. {@code me} é o próprio visualizador. */
    @GetMapping("/api/users/{id}/posts")
    public PaginaDePosts doUsuario(Visualizador visualizador, @PathVariable String id,
                                   @RequestParam(required = false) String cursor,
                                   @RequestParam(required = false) String limit) {
        String autorId = "me".equals(id) ? visualizador.id() : id;
        return pagina(listarPostsDoUsuario.executar(visualizador, autorId, cursor, limit));
    }

    /** SPEC RN29. */
    @GetMapping("/api/posts/feed")
    public PaginaDePosts feed(Visualizador visualizador,
                              @RequestParam(required = false) String cursor,
                              @RequestParam(required = false) String limit) {
        return pagina(verFeed.executar(visualizador, cursor, limit));
    }

    private PaginaDePosts pagina(Pagina<PostDetalhado> pagina) {
        return new PaginaDePosts(pagina.itens().stream().map(d -> PostResponse.de(d, imagens)).toList(),
                pagina.temMais(), pagina.proximoCursor());
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
        return new PostCriadoResponse(PostCriado.de(post, imagens));
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

    public record PostUnicoResponse(PostResponse post) {
    }

    /** Post recém-criado: autor e categorias como ids (SPEC 5.3). */
    public record PostCriado(@JsonProperty("_id") String id, String authorId, String institutionId, String text,
                             List<String> imageUrls, @JsonInclude(JsonInclude.Include.NON_NULL) String linkUrl,
                             boolean hasLink, List<String> categoryIds, int commentCount, Instant createdAt) {

        static PostCriado de(Post p, EnderecoDasImagens imagens) {
            return new PostCriado(p.id(), p.autorId(), p.instituicaoId(), p.texto(), imagens.urls(p.imagens()),
                    p.linkUrl(), p.temLink(), p.categoriaIds(), p.comentarios(), p.criadoEm());
        }
    }
}
