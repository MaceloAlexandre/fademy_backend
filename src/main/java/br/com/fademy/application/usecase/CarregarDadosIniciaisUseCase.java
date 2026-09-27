package br.com.fademy.application.usecase;

/**
 * Garante, de forma idempotente, a instituição inicial, as categorias e o admin (SPEC RN03).
 */
public interface CarregarDadosIniciaisUseCase {

    void executar(AdminInicial admin);

    record AdminInicial(String email, String senha, String nome) {
    }
}
