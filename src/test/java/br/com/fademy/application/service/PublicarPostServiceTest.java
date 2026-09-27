package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicarPostServiceTest {

    private final UsuarioGateway usuarios = mock();
    private final CategoriaGateway categorias = mock();
    private final ArmazenamentoGateway armazenamento = mock();
    private final PostGateway posts = mock();
    private final TransacaoGateway transacao = mock();
    private final PublicarPostService service =
            new PublicarPostService(usuarios, categorias, armazenamento, posts, transacao);

    private final Visualizador visualizador = new Visualizador("ana", Papel.ALUNO, "inst");
    private final ArquivoEnviado jpeg = new ArquivoEnviado(new byte[]{1}, "image/jpeg");

    @BeforeEach
    void autora() {
        when(usuarios.doVisualizador(visualizador)).thenReturn(Papel.ALUNO.criarUsuario(new DadosUsuario(
                "ana", "inst", "Ana", "a@x.com", "h", null, false, true, Instant.now())));
        when(categorias.existem(any())).thenReturn(true);
    }

    @Test
    void imagemInvalidaNaoEnviaNenhuma() {
        erro(List.of(jpeg, new ArquivoEnviado(new byte[]{1}, "image/gif")), "Invalid file type");
        verifyNoInteractions(armazenamento, posts);
    }

    @Test
    void categoriaInexistenteNaoEnviaImagens() {
        when(categorias.existem(any())).thenReturn(false);

        erro(List.of(jpeg), "Invalid category");
        verifyNoInteractions(armazenamento, posts);
    }

    @Test
    void maisDeQuatroImagens() {
        erro(Collections.nCopies(5, jpeg), "File upload error");
        verifyNoInteractions(armazenamento, posts);
    }

    private void erro(List<ArquivoEnviado> imagens, String mensagem) {
        assertThatThrownBy(() -> service.executar(visualizador, "texto", null, List.of("c1"), imagens))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(mensagem);
    }
}
