package br.com.fademy.infrastructure.config;

import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase.AdminInicial;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Roda a carga inicial (SPEC RN03) na subida da aplicação.
 */
@Component
public class CargaInicial implements ApplicationRunner {

    private final CarregarDadosIniciaisUseCase carregarDadosIniciais;
    private final AdminInicial admin;

    public CargaInicial(CarregarDadosIniciaisUseCase carregarDadosIniciais,
                        @Value("${fademy.admin.email}") String email,
                        @Value("${fademy.admin.senha}") String senha,
                        @Value("${fademy.admin.nome}") String nome) {
        this.carregarDadosIniciais = carregarDadosIniciais;
        this.admin = new AdminInicial(email, senha, nome);
    }

    @Override
    public void run(ApplicationArguments args) {
        carregarDadosIniciais.executar(admin);
    }
}
