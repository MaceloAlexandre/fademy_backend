package br.com.fademy.domain.usuario;

import br.com.fademy.domain.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PapelTest {

    @ParameterizedTest
    @CsvSource({
            "ADMIN,       Administrador",
            "COORDENADOR, Coordenador",
            "PROFESSOR,   Professor",
            "FUNCIONARIO, Funcionario",
            "ALUNO,       Aluno",
    })
    void fabricaCriaASubclasseDoPapel(Papel papel, String classe) {
        Usuario usuario = UsuarioTest.usuario(papel);

        assertThat(usuario.getClass().getSimpleName()).isEqualTo(classe);
        assertThat(usuario.papel()).isEqualTo(papel);
    }

    @ParameterizedTest
    @CsvSource({"ALUNO, 50", "PROFESSOR, 10", "COORDENADOR, 10", "FUNCIONARIO, 10", "ADMIN, 10"})
    void vagasMaximasPorConvite(Papel papel, int vagas) {
        assertThat(papel.vagasMaximasPorConvite()).isEqualTo(vagas);
    }

    @ParameterizedTest
    @EnumSource(Papel.class)
    void deTextoReconheceOTextoDaApi(Papel papel) {
        assertThat(Papel.deTexto(papel.texto())).isEqualTo(papel);
    }

    @Test
    void textosDaApi() {
        assertThat(Papel.ALUNO.texto()).isEqualTo("aluno");
        assertThat(Papel.COORDENADOR.texto()).isEqualTo("coordenador");
        assertThat(Papel.PROFESSOR.texto()).isEqualTo("professor");
        assertThat(Papel.FUNCIONARIO.texto()).isEqualTo("funcionario");
        assertThat(Papel.ADMIN.texto()).isEqualTo("admin");
    }

    @Test
    void textoDesconhecidoEhDadoInvalido() {
        assertThatThrownBy(() -> Papel.deTexto("reitor"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Invalid roleTarget");
    }
}
