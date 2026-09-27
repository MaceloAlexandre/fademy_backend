package br.com.fademy.infrastructure.config;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.service.AutenticarService;
import br.com.fademy.application.service.CarregarDadosIniciaisService;
import br.com.fademy.application.service.ConsultarConviteService;
import br.com.fademy.application.service.EnviarAvatarService;
import br.com.fademy.application.service.GerarConviteService;
import br.com.fademy.application.service.ListarCategoriasService;
import br.com.fademy.application.service.RegistrarViaConviteService;
import br.com.fademy.application.service.RevogarConviteService;
import br.com.fademy.application.service.VerificarSaudeService;
import br.com.fademy.application.usecase.AutenticarUseCase;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase;
import br.com.fademy.application.usecase.ConsultarConviteUseCase;
import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import br.com.fademy.application.usecase.GerarConviteUseCase;
import br.com.fademy.application.usecase.ListarCategoriasUseCase;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase;
import br.com.fademy.application.usecase.RevogarConviteUseCase;
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

    @Bean
    public AutenticarUseCase autenticar(UsuarioGateway usuarios, SenhaGateway senhas, TokenGateway tokens) {
        return new AutenticarService(usuarios, senhas, tokens);
    }

    @Bean
    public ListarCategoriasUseCase listarCategorias(CategoriaGateway categorias) {
        return new ListarCategoriasService(categorias);
    }

    @Bean
    public EnviarAvatarUseCase enviarAvatar(ArmazenamentoGateway armazenamento) {
        return new EnviarAvatarService(armazenamento);
    }

    @Bean
    public GerarConviteUseCase gerarConvite(UsuarioGateway usuarios, ConviteGateway convites) {
        return new GerarConviteService(usuarios, convites);
    }

    @Bean
    public RevogarConviteUseCase revogarConvite(UsuarioGateway usuarios, ConviteGateway convites) {
        return new RevogarConviteService(usuarios, convites);
    }

    @Bean
    public ConsultarConviteUseCase consultarConvite(ConviteGateway convites, InstituicaoGateway instituicoes) {
        return new ConsultarConviteService(convites, instituicoes);
    }

    @Bean
    public RegistrarViaConviteUseCase registrarViaConvite(ConviteGateway convites, UsuarioGateway usuarios,
                                                          ArmazenamentoGateway armazenamento, SenhaGateway senhas,
                                                          TransacaoGateway transacao,
                                                          NotificacaoGateway notificacoes) {
        return new RegistrarViaConviteService(convites, usuarios, armazenamento, senhas, transacao, notificacoes);
    }
}
