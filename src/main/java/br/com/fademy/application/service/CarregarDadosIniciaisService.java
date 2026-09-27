package br.com.fademy.application.service;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;

import java.time.Instant;

public class CarregarDadosIniciaisService implements CarregarDadosIniciaisUseCase {

    private final InstituicaoGateway instituicoes;
    private final CategoriaGateway categorias;
    private final UsuarioGateway usuarios;
    private final SenhaGateway senhas;

    public CarregarDadosIniciaisService(InstituicaoGateway instituicoes, CategoriaGateway categorias,
                                        UsuarioGateway usuarios, SenhaGateway senhas) {
        this.instituicoes = instituicoes;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Override
    public void executar(AdminInicial admin) {
        Instituicao catolica = instituicoes.garantir(
                new Instituicao(null, "Faculdade Católica da Paraíba", "catolica-pb", null, true, Instant.now()));

        categorias.garantir(Categoria.fixas());

        // Admin que já existe não é alterado: rodar de novo não troca a senha.
        if (usuarios.buscarPorEmail(admin.email()).isEmpty()) {
            usuarios.criar(Papel.ADMIN.criarUsuario(new DadosUsuario(
                    null, catolica.id(), admin.nome(), admin.email(), senhas.gerarHash(admin.senha()),
                    null, false, true, Instant.now())));
        }
    }
}
