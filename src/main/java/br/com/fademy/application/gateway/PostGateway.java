package br.com.fademy.application.gateway;

import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Usuario;

import java.util.List;
import java.util.Optional;

public interface PostGateway {

    Post criar(Post post);

    Optional<Post> buscarPorId(String id);

    void somarComentario(String postId);

    List<Post> doAutor(String autorId, String instituicaoId, String cursor, int quantidade);

    List<Post> feed(Usuario leitor, String cursor, int quantidade);
}
