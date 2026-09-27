package br.com.fademy.application.gateway;

import java.util.function.Supplier;

/**
 * Executa escritas no PostgreSQL numa única transação (SPEC RN46): se a ação lança exceção,
 * nada do que ela gravou fica.
 */
public interface TransacaoGateway {

    <T> T executar(Supplier<T> acao);

    /** Mesma coisa, para ações sem retorno. */
    default void executar(Runnable acao) {
        executar(() -> {
            acao.run();
            return null;
        });
    }
}
