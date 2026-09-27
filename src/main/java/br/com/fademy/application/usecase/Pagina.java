package br.com.fademy.application.usecase;

import java.util.List;
import java.util.function.Function;

/**
 * Uma página de uma lista paginada por cursor (SPEC 5.4).
 */
public record Pagina<T>(List<T> itens, boolean temMais, String proximoCursor) {

    /**
     * Monta a página a partir de uma consulta que pediu um item a mais que o limite: se ele veio,
     * há mais itens, e o cursor da próxima página é o id do último item mostrado.
     */
    public static <T> Pagina<T> de(List<T> umAMaisQueOLimite, int limite, Function<T, String> id) {
        boolean temMais = umAMaisQueOLimite.size() > limite;
        List<T> itens = temMais ? umAMaisQueOLimite.subList(0, limite) : umAMaisQueOLimite;
        return new Pagina<>(List.copyOf(itens), temMais, temMais ? id.apply(itens.getLast()) : null);
    }
}
