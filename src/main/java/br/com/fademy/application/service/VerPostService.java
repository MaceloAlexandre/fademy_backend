package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.PostDetalhado;
import br.com.fademy.application.usecase.VerPostUseCase;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public class VerPostService implements VerPostUseCase {

    private final UsuarioGateway usuarios;
    private final PostsVisiveis postsVisiveis;
    private final MontadorDePosts montador;

    public VerPostService(UsuarioGateway usuarios, PostGateway posts, BloqueioGateway bloqueios,
                          CategoriaGateway categorias) {
        this.usuarios = usuarios;
        this.postsVisiveis = new PostsVisiveis(posts, bloqueios);
        this.montador = new MontadorDePosts(usuarios, categorias);
    }

    @Override
    public PostDetalhado executar(Visualizador visualizador, String postId) {
        Post post = postsVisiveis.buscar(usuarios.doVisualizador(visualizador), postId);
        return montador.detalhar(List.of(post)).getFirst();
    }
}
