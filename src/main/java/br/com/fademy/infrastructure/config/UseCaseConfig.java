package br.com.fademy.infrastructure.config;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.service.CarregarDadosIniciaisService;
import br.com.fademy.application.service.VerificarSaudeService;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase;
import br.com.fademy.application.usecase.VerificarSaudeUseCase;
import br.com.fademy.infrastructure.mongo.MongoSaudeGateway;
import br.com.fademy.infrastructure.postgres.PostgresSaudeGateway;
import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Liga cada service aos seus gateways. Os services não têm anotação do Spring
 * (docs/ARQUITETURA.md, seção 4).
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public VerificarSaudeUseCase verificarSaude(DataSource dataSource, MongoClient mongo) {
        return new VerificarSaudeService(new PostgresSaudeGateway(dataSource), new MongoSaudeGateway(mongo));
    }

    @Bean
    public CarregarDadosIniciaisUseCase carregarDadosIniciais(InstituicaoGateway instituicoes,
                                                              CategoriaGateway categorias,
                                                              UsuarioGateway usuarios, SenhaGateway senhas) {
        return new CarregarDadosIniciaisService(instituicoes, categorias, usuarios, senhas);
    }
}
