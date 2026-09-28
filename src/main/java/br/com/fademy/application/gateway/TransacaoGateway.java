package br.com.fademy.application.gateway;

import java.util.function.Supplier;

public interface TransacaoGateway {

    <T> T executar(Supplier<T> acao);

    default void executar(Runnable acao) {
        executar(() -> {
            acao.run();
            return null;
        });
    }
}
