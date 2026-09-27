package br.com.fademy.application.service;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.application.usecase.PostDetalhado;
import br.com.fademy.application.usecase.VerFeedUseCase;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Visualizador;

public class VerFeedService implements VerFeedUseCase {

    private final UsuarioGateway usuarios;
    private final PostGateway posts;
    private final MontadorDePosts montador;

    public VerFeedService(UsuarioGateway usuarios, PostGateway posts, CategoriaGateway categorias) {
        this.usuarios = usuarios;
        this.posts = posts;
        this.montador = new MontadorDePosts(usuarios, categorias);
    }

    @Override
    public Pagina<PostDetalhado> executar(Visualizador visualizador, String cursor, String limite) {
        int tamanho = Limites.ler(limite, 20, 50);
        Pagina<Post> pagina = Pagina.de(
                posts.feed(usuarios.doVisualizador(visualizador), cursor, tamanho + 1), tamanho, Post::id);
        return new Pagina<>(montador.detalhar(pagina.itens()), pagina.temMais(), pagina.proximoCursor());
    }
}
