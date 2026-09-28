package br.com.fademy.application.usecase;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> itens, boolean temMais, String proximoCursor) {

    public static <T> Pagina<T> de(List<T> umAMaisQueOLimite, int limite, Function<T, String> id) {
        boolean temMais = umAMaisQueOLimite.size() > limite;
        List<T> itens = temMais ? umAMaisQueOLimite.subList(0, limite) : umAMaisQueOLimite;
        return new Pagina<>(List.copyOf(itens), temMais, temMais ? id.apply(itens.getLast()) : null);
    }
}
