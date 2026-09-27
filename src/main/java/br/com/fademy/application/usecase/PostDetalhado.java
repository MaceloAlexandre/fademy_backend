package br.com.fademy.application.usecase;

import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.usuario.Usuario;

import java.util.List;

/**
 * Post com autor e categorias já carregados, como as telas mostram.
 */
public record PostDetalhado(Post post, Usuario autor, List<Categoria> categorias) {
}
