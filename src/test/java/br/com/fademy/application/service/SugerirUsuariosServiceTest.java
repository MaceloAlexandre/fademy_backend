package br.com.fademy.application.service;

import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SugerirUsuariosServiceTest {

    private final UsuarioGateway usuarios = mock();
    private final SugerirUsuariosService service = new SugerirUsuariosService(usuarios);
    private final Visualizador visualizador = new Visualizador("eu", Papel.ALUNO, "inst");

    @Test
    void equipePrimeiroMantendoAOrdemDeCadastro() {
        Usuario eu = usuario("eu", Papel.ALUNO);
        when(usuarios.doVisualizador(visualizador)).thenReturn(eu);
        // Chegam do mais recente para o mais antigo.
        when(usuarios.candidatosASugestao(eu)).thenReturn(List.of(
                usuario("aluno1", Papel.ALUNO), usuario("prof1", Papel.PROFESSOR), usuario("aluno2", Papel.ALUNO),
                usuario("coord", Papel.COORDENADOR), usuario("admin", Papel.ADMIN)));

        assertThat(service.executar(visualizador, null)).extracting(Usuario::id)
                .containsExactly("prof1", "coord", "admin", "aluno1", "aluno2");
        assertThat(service.executar(visualizador, "2")).extracting(Usuario::id).containsExactly("prof1", "coord");
    }

    private static Usuario usuario(String id, Papel papel) {
        return papel.criarUsuario(new DadosUsuario(id, "inst", id, id + "@x.com", "h", null, false, true, Instant.now()));
    }
}
