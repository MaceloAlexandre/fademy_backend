package br.com.fademy.infrastructure.postgres;

import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase.AdminInicial;
import br.com.fademy.domain.excecao.ConflitoException;
import br.com.fademy.domain.usuario.Administrador;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CargaInicialIntegracaoTest extends IntegracaoTest {

    @Autowired
    JdbcClient jdbc;

    @Autowired
    UsuarioGateway usuarios;

    @Autowired
    SenhaGateway senhas;

    @Autowired
    CarregarDadosIniciaisUseCase carregarDadosIniciais;

    @Autowired
    ArmazenamentoGateway armazenamento;

    @Test
    void adminNasceComAvatarDeIniciaisNoArmazenamento() {
        String chave = usuarios.buscarPorEmail("admin@fademy.com").orElseThrow().avatarChave();

        assertThat(chave).startsWith("avatars/").endsWith(".png");
        assertThat(armazenamento.existe(chave)).isTrue();
    }

    @Test
    void subidaCriaInstituicaoCategoriasEAdmin() {
        assertThat(contar("select count(*) from instituicao where slug = 'catolica-pb' and ativa"
                + " and nome = 'Faculdade Católica da Paraíba'")).isEqualTo(1);
        assertThat(contar("select count(*) from categoria where tipo = 'content'")).isEqualTo(4);
        assertThat(contar("select count(*) from categoria where tipo = 'area'")).isEqualTo(6);
        assertThat(contar("select count(*) from categoria where slug = 'ciencia-da-computacao'")).isEqualTo(1);
        assertThat(contar("select count(*) from usuario u join instituicao i on i.id = u.instituicao_id"
                + " where u.email = 'admin@fademy.com' and u.papel = 'admin' and i.slug = 'catolica-pb'"))
                .isEqualTo(1);
    }

    @Test
    void rodarDeNovoNaoDuplicaNemTrocaASenha() {
        jdbc.sql("update usuario set senha_hash = 'senha-trocada' where email = 'admin@fademy.com'").update();

        carregarDadosIniciais.executar(new AdminInicial("admin@fademy.com", "changeme", "Admin"));

        assertThat(contar("select count(*) from instituicao where slug = 'catolica-pb'")).isEqualTo(1);
        assertThat(contar("select count(*) from categoria")).isEqualTo(10);
        assertThat(contar("select count(*) from usuario where email = 'admin@fademy.com'")).isEqualTo(1);
        assertThat(jdbc.sql("select senha_hash from usuario where email = 'admin@fademy.com'")
                .query(String.class).single()).isEqualTo("senha-trocada");

        jdbc.sql("update usuario set senha_hash = :hash where email = 'admin@fademy.com'")
                .param("hash", senhas.gerarHash("changeme")).update();
    }

    @Test
    void senhaDoAdminGuardadaComBcrypt() {
        Usuario admin = usuarios.buscarPorEmail("admin@fademy.com").orElseThrow();

        assertThat(admin.senhaHash()).startsWith("$2a$");
        assertThat(senhas.confere("changeme", admin.senhaHash())).isTrue();
    }

    @Test
    void emailRepetidoComOutraCaixaEhConflito() {
        usuarios.criar(novoAluno("Repetido@x.com"));

        assertThatThrownBy(() -> usuarios.criar(novoAluno("repetido@X.com")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Este e-mail já está em uso");
    }

    @Test
    void usuarioLidoDeVoltaVemComoSubclasseEIdV7() {
        Usuario admin = usuarios.buscarPorEmail("ADMIN@fademy.com").orElseThrow();

        assertThat(admin).isInstanceOf(Administrador.class);
        assertThat(UUID.fromString(admin.id()).version()).isEqualTo(7);
    }

    private Usuario novoAluno(String email) {
        String instituicao = jdbc.sql("select id from instituicao where slug = 'catolica-pb'")
                .query(String.class).single();
        return Papel.ALUNO.criarUsuario(new DadosUsuario(
                null, instituicao, "Aluno", email, "hash", null, false, true, Instant.now()));
    }

    private long contar(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }
}
