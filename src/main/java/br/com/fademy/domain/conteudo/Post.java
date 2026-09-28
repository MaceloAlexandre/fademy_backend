package br.com.fademy.domain.conteudo;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Usuario;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Post {

    private final String id;
    private final String autorId;
    private final String instituicaoId;
    private final String texto;
    private final List<String> imagens;
    private final String linkUrl;
    private final List<String> categoriaIds;
    private final int comentarios;
    private final Instant criadoEm;

    public Post(String id, String autorId, String instituicaoId, String texto, List<String> imagens, String linkUrl,
                List<String> categoriaIds, int comentarios, Instant criadoEm) {
        this.id = id;
        this.autorId = autorId;
        this.instituicaoId = instituicaoId;
        this.texto = texto;
        this.imagens = new ArrayList<>(imagens);
        this.linkUrl = linkUrl;
        this.categoriaIds = List.copyOf(categoriaIds);
        this.comentarios = comentarios;
        this.criadoEm = criadoEm;
    }

    public static Post publicar(Usuario autor, String texto, String linkUrl, List<String> categoriaIds, Instant agora) {
        if (texto == null || texto.isBlank()) {
            throw new DadosInvalidosException("text is required");
        }
        boolean temLink = linkUrl != null && !linkUrl.isBlank();
        if (temLink && !autor.podePublicarLink()) {
            throw new AcessoNegadoException("Insufficient permissions");
        }
        List<String> categorias = categoriaIds == null ? List.of() : categoriaIds.stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
        return new Post(null, autor.id(), autor.instituicaoId(), texto.trim(), List.of(),
                temLink ? linkUrl.trim() : null, categorias, 0, agora);
    }

    public void anexarImagem(String chave) {
        imagens.add(chave);
    }

    public boolean daInstituicao(String instituicaoId) {
        return this.instituicaoId.equals(instituicaoId);
    }

    public boolean notificaAutorSobreComentarioDe(Usuario comentarista) {
        return !autorId.equals(comentarista.id());
    }

    public boolean temLink() {
        return linkUrl != null;
    }

    public String id() {
        return id;
    }

    public String autorId() {
        return autorId;
    }

    public String instituicaoId() {
        return instituicaoId;
    }

    public String texto() {
        return texto;
    }

    public List<String> imagens() {
        return List.copyOf(imagens);
    }

    public String linkUrl() {
        return linkUrl;
    }

    public List<String> categoriaIds() {
        return categoriaIds;
    }

    public int comentarios() {
        return comentarios;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}
