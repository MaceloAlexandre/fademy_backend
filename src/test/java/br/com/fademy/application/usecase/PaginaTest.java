package br.com.fademy.application.usecase;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaTest {

    @Test
    void itemAMaisIndicaQueHaProximaPagina() {
        Pagina<String> pagina = Pagina.de(List.of("c", "b", "a"), 2, Function.identity());

        assertThat(pagina.itens()).containsExactly("c", "b");
        assertThat(pagina.temMais()).isTrue();
        assertThat(pagina.proximoCursor()).isEqualTo("b");
    }

    @Test
    void ultimaPagina() {
        Pagina<String> pagina = Pagina.de(List.of("b", "a"), 2, Function.identity());

        assertThat(pagina.itens()).containsExactly("b", "a");
        assertThat(pagina.temMais()).isFalse();
        assertThat(pagina.proximoCursor()).isNull();
    }
}
