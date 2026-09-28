package br.com.fademy.application.service;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.PostDetalhado;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.usuario.Usuario;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class MontadorDePosts {

    private final UsuarioGateway usuarios;
    private final CategoriaGateway categorias;

    MontadorDePosts(UsuarioGateway usuarios, CategoriaGateway categorias) {
        this.usuarios = usuarios;
        this.categorias = categorias;
    }

    List<PostDetalhado> detalhar(List<Post> posts) {
        Map<String, Usuario> autores = usuarios.buscarPorIds(
                        posts.stream().map(Post::autorId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Usuario::id, Function.identity()));
        Map<String, Categoria> todasAsCategorias = categorias.buscarPorIds(
                        posts.stream().flatMap(p -> p.categoriaIds().stream()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Categoria::id, Function.identity()));

        return posts.stream().map(post -> new PostDetalhado(post, autores.get(post.autorId()),
                post.categoriaIds().stream().map(todasAsCategorias::get).filter(Objects::nonNull).toList())).toList();
    }
}
