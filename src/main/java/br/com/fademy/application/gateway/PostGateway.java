package br.com.fademy.application.gateway;

import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Usuario;

import java.util.List;

public interface PostGateway {

    /** Grava o post com suas categorias e o devolve com id. */
    Post criar(Post post);

    /**
     * Feed do leitor: posts de quem ele segue com follow aceito, na instituição dele, sem autores
     * com bloqueio, do mais novo para o mais antigo, só os anteriores ao cursor (SPEC RN29).
     *
     * @throws br.com.fademy.domain.excecao.DadosInvalidosException se o cursor não é um id válido
     */
    List<Post> feed(Usuario leitor, String cursor, int quantidade);
}
