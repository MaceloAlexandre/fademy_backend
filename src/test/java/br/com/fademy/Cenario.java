package br.com.fademy;

import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Monta dados para os testes de integração: instituições, usuários de cada papel e tokens.
 * Cada chamada cria registros novos (e-mails únicos), então os testes não interferem entre si.
 */
@Component
public class Cenario {

    public static final String SENHA = "senha123";

    private final InstituicaoGateway instituicoes;
    private final UsuarioGateway usuarios;
    private final SenhaGateway senhas;
    private final TokenGateway tokens;
    private final PostGateway posts;

    public Cenario(InstituicaoGateway instituicoes, UsuarioGateway usuarios, SenhaGateway senhas, TokenGateway tokens,
                   PostGateway posts) {
        this.instituicoes = instituicoes;
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.posts = posts;
    }

    /** Post do autor, gravado direto (sem passar pela API). */
    public Post post(Usuario autor, String texto, String... categoriaIds) {
        return posts.criar(Post.publicar(autor, texto, null, List.of(categoriaIds), Instant.now()));
    }

    /** A instituição da carga inicial (mesmo nome, para o upsert não alterá-la). */
    public Instituicao catolica() {
        return instituicoes.garantir(new Instituicao(
                null, "Faculdade Católica da Paraíba", "catolica-pb", null, true, Instant.now()));
    }

    public Instituicao instituicao(String slug) {
        return instituicoes.garantir(new Instituicao(null, "Instituição " + slug, slug, null, true, Instant.now()));
    }

    public Usuario usuario(Papel papel) {
        return usuario(papel, catolica());
    }

    public Usuario usuario(Papel papel, Instituicao instituicao) {
        return usuario(papel, instituicao, false);
    }

    public Usuario privado(Papel papel) {
        return usuario(papel, catolica(), true);
    }

    public Usuario usuario(Papel papel, Instituicao instituicao, boolean privado) {
        return usuario(papel, instituicao, "Usuário " + papel.texto(), privado);
    }

    public Usuario usuario(Papel papel, Instituicao instituicao, String nome, boolean privado) {
        String email = papel.texto() + "-" + UUID.randomUUID() + "@teste.com";
        return usuarios.criar(papel.criarUsuario(new DadosUsuario(
                null, instituicao.id(), nome, email, senhas.gerarHash(SENHA),
                "http://avatar", privado, true, Instant.now())));
    }

    /** Instituição nova, só deste teste. */
    public Instituicao instituicaoNova() {
        return instituicao("teste-" + UUID.randomUUID());
    }

    public String token(Usuario usuario) {
        return tokens.gerar(usuario);
    }

    /** Valor do cabeçalho Authorization para o usuário. */
    public String bearer(Usuario usuario) {
        return "Bearer " + token(usuario);
    }
}
