package br.com.fademy.domain.conteudo;

import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Usuario;

import java.time.Instant;

/**
 * Comentário num post (SPEC RN32).
 */
public record Comentario(String id, String postId, String autorId, String instituicaoId, String texto,
                         Instant criadoEm) {

    public static Comentario escrever(Usuario autor, String postId, String texto, Instant agora) {
        if (texto == null || texto.isBlank()) {
            throw new DadosInvalidosException("text is required");
        }
        return new Comentario(null, postId, autor.id(), autor.instituicaoId(), texto.trim(), agora);
    }
}
