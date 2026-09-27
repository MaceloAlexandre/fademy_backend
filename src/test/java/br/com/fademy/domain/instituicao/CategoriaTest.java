package br.com.fademy.domain.instituicao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CategoriaTest {

    @Test
    void categoriasFixasDaRN02() {
        assertThat(Categoria.fixas())
                .extracting(Categoria::nome, Categoria::slug, Categoria::tipo)
                .containsExactly(
                        tuple("Projetos", "projetos", TipoCategoria.CONTENT),
                        tuple("Aulas", "aulas", TipoCategoria.CONTENT),
                        tuple("Dia a dia", "dia-a-dia", TipoCategoria.CONTENT),
                        tuple("Meme", "meme", TipoCategoria.CONTENT),
                        tuple("Ciência da Computação", "ciencia-da-computacao", TipoCategoria.AREA),
                        tuple("Direito", "direito", TipoCategoria.AREA),
                        tuple("Marketing", "marketing", TipoCategoria.AREA),
                        tuple("Filosofia", "filosofia", TipoCategoria.AREA),
                        tuple("Educação Física", "educacao-fisica", TipoCategoria.AREA),
                        tuple("NEPA", "nepa", TipoCategoria.AREA));
    }
}
