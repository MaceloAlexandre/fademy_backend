package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.PublicarPostUseCase;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.midia.Imagem;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;
import java.util.List;

public class PublicarPostService implements PublicarPostUseCase {

    private static final int MAXIMO_DE_IMAGENS = 4;

    private final UsuarioGateway usuarios;
    private final CategoriaGateway categorias;
    private final ArmazenamentoGateway armazenamento;
    private final PostGateway posts;
    private final TransacaoGateway transacao;

    public PublicarPostService(UsuarioGateway usuarios, CategoriaGateway categorias,
                               ArmazenamentoGateway armazenamento, PostGateway posts, TransacaoGateway transacao) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.armazenamento = armazenamento;
        this.posts = posts;
        this.transacao = transacao;
    }

    @Override
    public Post executar(Visualizador visualizador, String texto, String linkUrl, List<String> categoriaIds,
                         List<ArquivoEnviado> imagens) {
        if (imagens.size() > MAXIMO_DE_IMAGENS) {
            throw new DadosInvalidosException("File upload error");
        }
        Post post = Post.publicar(usuarios.doVisualizador(visualizador), texto, linkUrl, categoriaIds, Instant.now());

        if (imagens.stream().anyMatch(arquivo -> !Imagem.aceita(arquivo.tipo()))) {
            throw new DadosInvalidosException("Invalid file type");
        }
        if (!categorias.existem(post.categoriaIds())) {
            throw new DadosInvalidosException("Invalid category");
        }

        for (ArquivoEnviado arquivo : imagens) {
            Imagem imagem = new Imagem(arquivo.conteudo(), arquivo.tipo());
            String chave = imagem.novaChave("posts");
            armazenamento.enviar(chave, imagem);
            post.anexarImagem(chave);
        }
        return transacao.executar(() -> posts.criar(post));
    }
}
