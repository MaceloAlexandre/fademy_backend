package br.com.fademy.application.service;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase.AdminInicial;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.instituicao.Instituicao;
import br.com.fademy.domain.usuario.Administrador;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CarregarDadosIniciaisServiceTest {

    private final InstituicaoGateway instituicoes = mock();
    private final CategoriaGateway categorias = mock();
    private final UsuarioGateway usuarios = mock();
    private final SenhaGateway senhas = mock();
    private final CarregarDadosIniciaisService service =
            new CarregarDadosIniciaisService(instituicoes, categorias, usuarios, senhas);

    private final AdminInicial admin = new AdminInicial("admin@fademy.com", "changeme", "Admin");

    @BeforeEach
    void instituicaoGarantida() {
        when(instituicoes.garantir(any())).thenAnswer(chamada -> {
            Instituicao i = chamada.getArgument(0);
            return new Instituicao("id-catolica", i.nome(), i.slug(), i.logoUrl(), i.ativa(), i.criadaEm());
        });
    }

    @Test
    void garanteInstituicaoCategoriasEAdmin() {
        when(usuarios.buscarPorEmail("admin@fademy.com")).thenReturn(Optional.empty());
        when(senhas.gerarHash("changeme")).thenReturn("hash");

        service.executar(admin);

        ArgumentCaptor<Instituicao> instituicao = ArgumentCaptor.captor();
        verify(instituicoes).garantir(instituicao.capture());
        assertThat(instituicao.getValue().slug()).isEqualTo("catolica-pb");
        assertThat(instituicao.getValue().nome()).isEqualTo("Faculdade Católica da Paraíba");

        verify(categorias).garantir(Categoria.fixas());

        ArgumentCaptor<Usuario> criado = ArgumentCaptor.captor();
        verify(usuarios).criar(criado.capture());
        assertThat(criado.getValue()).isInstanceOf(Administrador.class);
        assertThat(criado.getValue().email()).isEqualTo("admin@fademy.com");
        assertThat(criado.getValue().nome()).isEqualTo("Admin");
        assertThat(criado.getValue().senhaHash()).isEqualTo("hash");
        assertThat(criado.getValue().instituicaoId()).isEqualTo("id-catolica");
        assertThat(criado.getValue().ativo()).isTrue();
    }

    @Test
    void adminQueJaExisteNaoEhAlterado() {
        Usuario existente = mock();
        when(usuarios.buscarPorEmail("admin@fademy.com")).thenReturn(Optional.of(existente));

        service.executar(admin);

        verify(usuarios, never()).criar(any());
        verify(senhas, never()).gerarHash(any());
    }
}
