package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.ComentarioGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.ComentarUseCase;
import br.com.fademy.application.usecase.ComentarioDetalhado;
import br.com.fademy.domain.conteudo.Comentario;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ComentarService implements ComentarUseCase {

    private final UsuarioGateway usuarios;
    private final PostGateway posts;
    private final ComentarioGateway comentarios;
    private final NotificacaoGateway notificacoes;
    private final TransacaoGateway transacao;
    private final PostsVisiveis postsVisiveis;

    public ComentarService(UsuarioGateway usuarios, PostGateway posts, BloqueioGateway bloqueios,
                           ComentarioGateway comentarios, NotificacaoGateway notificacoes, TransacaoGateway transacao) {
        this.usuarios = usuarios;
        this.posts = posts;
        this.comentarios = comentarios;
        this.notificacoes = notificacoes;
        this.transacao = transacao;
        this.postsVisiveis = new PostsVisiveis(posts, bloqueios);
    }

    @Override
    public ComentarioDetalhado comentar(Visualizador visualizador, String postId, String texto) {
        Usuario eu = usuarios.doVisualizador(visualizador);
        Instant agora = Instant.now();
        Comentario novo = Comentario.escrever(eu, postId, texto, agora);
        Post post = postsVisiveis.buscar(eu, postId);

        // Comentário e contador juntos (SPEC RN46); a notificação depois do commit (RN47).
        Comentario comentario = transacao.executar(() -> {
            Comentario criado = comentarios.criar(novo);
            posts.somarComentario(post.id());
            return criado;
        });
        if (post.notificaAutorSobreComentarioDe(eu)) {
            notificacoes.criar(Notificacao.nova(
                    TipoNotificacao.POST_COMMENT, post.autorId(), eu.id(), post.id(), agora));
        }
        return new ComentarioDetalhado(comentario, eu);
    }

    @Override
    public List<ComentarioDetalhado> listar(Visualizador visualizador, String postId) {
        Post post = postsVisiveis.buscar(usuarios.doVisualizador(visualizador), postId);
        List<Comentario> doPost = comentarios.doPost(post.id());
        Map<String, Usuario> autores = usuarios.buscarPorIds(
                        doPost.stream().map(Comentario::autorId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Usuario::id, Function.identity()));
        return doPost.stream().map(c -> new ComentarioDetalhado(c, autores.get(c.autorId()))).toList();
    }
}
