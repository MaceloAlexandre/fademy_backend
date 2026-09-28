package br.com.fademy.application.service;

import br.com.fademy.application.gateway.AvatarPadraoGateway;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;

import java.time.Instant;
import java.util.Optional;

public class CarregarDadosIniciaisService implements CarregarDadosIniciaisUseCase {

    private final InstituicaoGateway instituicoes;
    private final CategoriaGateway categorias;
    private final UsuarioGateway usuarios;
    private final SenhaGateway senhas;
    private final AvatarPadraoGateway avatares;

    public CarregarDadosIniciaisService(InstituicaoGateway instituicoes, CategoriaGateway categorias,
                                        UsuarioGateway usuarios, SenhaGateway senhas, AvatarPadraoGateway avatares) {
        this.instituicoes = instituicoes;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.avatares = avatares;
    }

    @Override
    public void executar(AdminInicial admin) {
        Instituicao catolica = instituicoes.garantir(
                new Instituicao(null, "Faculdade Católica da Paraíba", "catolica-pb", null, true, Instant.now()));

        categorias.garantir(Categoria.fixas());

        // Admin que já existe não tem a senha trocada; só ganha a foto padrão se não tiver foto (RN03).
        Optional<Usuario> existente = usuarios.buscarPorEmail(admin.email());
        if (existente.isEmpty()) {
            usuarios.criar(Papel.ADMIN.criarUsuario(new DadosUsuario(
                    null, catolica.id(), admin.nome(), admin.email(), senhas.gerarHash(admin.senha()),
                    avatares.criar(admin.nome()).orElse(null), false, true, Instant.now())));
        } else if (existente.get().avatarChave() == null) {
            Usuario semFoto = existente.get();
            avatares.criar(semFoto.nome()).ifPresent(chave -> {
                semFoto.trocarAvatar(chave);
                usuarios.atualizar(semFoto);
            });
        }
    }
}
