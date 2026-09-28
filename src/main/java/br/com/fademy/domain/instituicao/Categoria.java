package br.com.fademy.domain.instituicao;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public record Categoria(String id, String nome, String slug, TipoCategoria tipo) {

    public static List<Categoria> fixas() {
        return List.of(
                nova("Projetos", TipoCategoria.CONTENT),
                nova("Aulas", TipoCategoria.CONTENT),
                nova("Dia a dia", TipoCategoria.CONTENT),
                nova("Meme", TipoCategoria.CONTENT),
                nova("Ciência da Computação", TipoCategoria.AREA),
                nova("Direito", TipoCategoria.AREA),
                nova("Marketing", TipoCategoria.AREA),
                nova("Filosofia", TipoCategoria.AREA),
                nova("Educação Física", TipoCategoria.AREA),
                nova("NEPA", TipoCategoria.AREA));
    }

    static Categoria nova(String nome, TipoCategoria tipo) {
        return new Categoria(null, nome, slug(nome), tipo);
    }

    static String slug(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "");
    }
}
