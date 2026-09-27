package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.ListarPostsDoUsuarioUseCase;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.application.usecase.PostDetalhado;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.Optional;

public class ListarPostsDoUsuarioService implements ListarPostsDoUsuarioUseCase {

    private final UsuarioGateway usuarios;
    private final PostGateway posts;
    private final SeguirGateway seguires;
    private final BloqueioGateway bloqueios;
    private final MontadorDePosts montador;

    public ListarPostsDoUsuarioService(UsuarioGateway usuarios, PostGateway posts, SeguirGateway seguires,
                                       BloqueioGateway bloqueios, CategoriaGateway categorias) {
        this.usuarios = usuarios;
        this.posts = posts;
        this.seguires = seguires;
        this.bloqueios = bloqueios;
        this.montador = new MontadorDePosts(usuarios, categorias);
    }

    @Override
    public Pagina<PostDetalhado> executar(Visualizador visualizador, String autorId, String cursor, String limite) {
        Usuario eu = usuarios.doVisualizador(visualizador);
        if (bloqueios.bloqueou(autorId, eu.id())) {
            throw new AcessoNegadoException("Forbidden");
        }
        // Autor inexistente ou de outra instituição simplesmente não tem posts para mostrar.
        Optional<Usuario> autor = usuarios.buscarPorId(autorId).filter(eu::mesmaInstituicao);
        if (autor.isPresent()) {
            boolean seguidoPorMim = seguires.buscar(eu.id(), autorId)
                    .map(Seguir::status).filter(StatusSeguir.ACCEPTED::equals).isPresent();
            if (!autor.get().mostraConteudoPara(eu.id(), seguidoPorMim)) {
                throw new AcessoNegadoException("Forbidden");
            }
        }

        int tamanho = Limites.ler(limite, 20, 50);
        Pagina<Post> pagina = Pagina.de(
                posts.doAutor(autorId, eu.instituicaoId(), cursor, tamanho + 1), tamanho, Post::id);
        return new Pagina<>(montador.detalhar(pagina.itens()), pagina.temMais(), pagina.proximoCursor());
    }
}
