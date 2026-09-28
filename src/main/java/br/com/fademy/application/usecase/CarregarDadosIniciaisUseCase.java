package br.com.fademy.application.usecase;

public interface CarregarDadosIniciaisUseCase {

    void executar(AdminInicial admin);

    record AdminInicial(String email, String senha, String nome) {
    }
}
