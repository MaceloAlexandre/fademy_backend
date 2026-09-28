package br.com.fademy.infrastructure.config;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.AvatarPadraoGateway;
import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.ComentarioGateway;
import br.com.fademy.application.gateway.ConversaGateway;
import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.application.gateway.MensagemGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.PostGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.TempoRealGateway;
import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.service.AceitarPedidoService;
import br.com.fademy.application.service.AutenticarService;
import br.com.fademy.application.service.BloquearUsuarioService;
import br.com.fademy.application.service.BuscarUsuariosService;
import br.com.fademy.application.service.CarregarDadosIniciaisService;
import br.com.fademy.application.service.ComentarService;
import br.com.fademy.application.service.ConsultarConviteService;
import br.com.fademy.application.service.DeixarDeSeguirService;
import br.com.fademy.application.service.DesbloquearUsuarioService;
import br.com.fademy.application.service.EditarPerfilService;
import br.com.fademy.application.service.EnviarAvatarService;
import br.com.fademy.application.service.VerImagemService;
import br.com.fademy.application.service.GerarConviteService;
import br.com.fademy.application.service.IniciarConversaService;
import br.com.fademy.application.service.LerNotificacaoService;
import br.com.fademy.application.service.ListarCategoriasService;
import br.com.fademy.application.service.ListarConversasService;
import br.com.fademy.application.service.ListarNotificacoesService;
import br.com.fademy.application.service.ListarPostsDoUsuarioService;
import br.com.fademy.application.service.MensagensService;
import br.com.fademy.application.service.PublicarPostService;
import br.com.fademy.application.service.RegistrarViaConviteService;
import br.com.fademy.application.service.RejeitarPedidoService;
import br.com.fademy.application.service.RevogarConviteService;
import br.com.fademy.application.service.SeguirUsuarioService;
import br.com.fademy.application.service.SugerirUsuariosService;
import br.com.fademy.application.service.VerFeedService;
import br.com.fademy.application.service.VerPainelAdminService;
import br.com.fademy.application.service.VerPerfilService;
import br.com.fademy.application.service.VerPostService;
import br.com.fademy.application.service.VerificarSaudeService;
import br.com.fademy.application.usecase.AceitarPedidoUseCase;
import br.com.fademy.application.usecase.AutenticarUseCase;
import br.com.fademy.application.usecase.BloquearUsuarioUseCase;
import br.com.fademy.application.usecase.BuscarUsuariosUseCase;
import br.com.fademy.application.usecase.CarregarDadosIniciaisUseCase;
import br.com.fademy.application.usecase.ComentarUseCase;
import br.com.fademy.application.usecase.ConsultarConviteUseCase;
import br.com.fademy.application.usecase.DeixarDeSeguirUseCase;
import br.com.fademy.application.usecase.DesbloquearUsuarioUseCase;
import br.com.fademy.application.usecase.EditarPerfilUseCase;
import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import br.com.fademy.application.usecase.VerImagemUseCase;
import br.com.fademy.application.usecase.GerarConviteUseCase;
import br.com.fademy.application.usecase.IniciarConversaUseCase;
import br.com.fademy.application.usecase.LerNotificacaoUseCase;
import br.com.fademy.application.usecase.ListarCategoriasUseCase;
import br.com.fademy.application.usecase.ListarConversasUseCase;
import br.com.fademy.application.usecase.ListarNotificacoesUseCase;
import br.com.fademy.application.usecase.ListarPostsDoUsuarioUseCase;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.PublicarPostUseCase;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase;
import br.com.fademy.application.usecase.RejeitarPedidoUseCase;
import br.com.fademy.application.usecase.RevogarConviteUseCase;
import br.com.fademy.application.usecase.SeguirUsuarioUseCase;
import br.com.fademy.application.usecase.SugerirUsuariosUseCase;
import br.com.fademy.application.usecase.VerFeedUseCase;
import br.com.fademy.application.usecase.VerPainelAdminUseCase;
import br.com.fademy.application.usecase.VerPerfilUseCase;
import br.com.fademy.application.usecase.VerPostUseCase;
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
                                                              UsuarioGateway usuarios, SenhaGateway senhas,
                                                              AvatarPadraoGateway avatares) {
        return new CarregarDadosIniciaisService(instituicoes, categorias, usuarios, senhas, avatares);
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
    public VerImagemUseCase verImagem(ArmazenamentoGateway armazenamento) {
        return new VerImagemService(armazenamento);
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

    @Bean
    public SeguirUsuarioUseCase seguirUsuario(UsuarioGateway usuarios, SeguirGateway seguires,
                                              BloqueioGateway bloqueios, NotificacaoGateway notificacoes) {
        return new SeguirUsuarioService(usuarios, seguires, bloqueios, notificacoes);
    }

    @Bean
    public DeixarDeSeguirUseCase deixarDeSeguir(SeguirGateway seguires) {
        return new DeixarDeSeguirService(seguires);
    }

    @Bean
    public AceitarPedidoUseCase aceitarPedido(SeguirGateway seguires, NotificacaoGateway notificacoes) {
        return new AceitarPedidoService(seguires, notificacoes);
    }

    @Bean
    public RejeitarPedidoUseCase rejeitarPedido(SeguirGateway seguires) {
        return new RejeitarPedidoService(seguires);
    }

    @Bean
    public BloquearUsuarioUseCase bloquearUsuario(UsuarioGateway usuarios, SeguirGateway seguires,
                                                  BloqueioGateway bloqueios, NotificacaoGateway notificacoes,
                                                  TransacaoGateway transacao) {
        return new BloquearUsuarioService(usuarios, seguires, bloqueios, notificacoes, transacao);
    }

    @Bean
    public DesbloquearUsuarioUseCase desbloquearUsuario(BloqueioGateway bloqueios) {
        return new DesbloquearUsuarioService(bloqueios);
    }

    @Bean
    public VerPerfilUseCase verPerfil(UsuarioGateway usuarios, SeguirGateway seguires, BloqueioGateway bloqueios) {
        return new VerPerfilService(usuarios, seguires, bloqueios);
    }

    @Bean
    public EditarPerfilUseCase editarPerfil(UsuarioGateway usuarios, ArmazenamentoGateway armazenamento) {
        return new EditarPerfilService(usuarios, armazenamento);
    }

    @Bean
    public BuscarUsuariosUseCase buscarUsuarios(UsuarioGateway usuarios) {
        return new BuscarUsuariosService(usuarios);
    }

    @Bean
    public SugerirUsuariosUseCase sugerirUsuarios(UsuarioGateway usuarios) {
        return new SugerirUsuariosService(usuarios);
    }

    @Bean
    public ListarNotificacoesUseCase listarNotificacoes(NotificacaoGateway notificacoes, UsuarioGateway usuarios,
                                                        SeguirGateway seguires) {
        return new ListarNotificacoesService(notificacoes, usuarios, seguires);
    }

    @Bean
    public LerNotificacaoUseCase lerNotificacao(NotificacaoGateway notificacoes) {
        return new LerNotificacaoService(notificacoes);
    }

    @Bean
    public PublicarPostUseCase publicarPost(UsuarioGateway usuarios, CategoriaGateway categorias,
                                            ArmazenamentoGateway armazenamento, PostGateway posts,
                                            TransacaoGateway transacao) {
        return new PublicarPostService(usuarios, categorias, armazenamento, posts, transacao);
    }

    @Bean
    public VerFeedUseCase verFeed(UsuarioGateway usuarios, PostGateway posts, CategoriaGateway categorias) {
        return new VerFeedService(usuarios, posts, categorias);
    }

    @Bean
    public VerPostUseCase verPost(UsuarioGateway usuarios, PostGateway posts, BloqueioGateway bloqueios,
                                  CategoriaGateway categorias) {
        return new VerPostService(usuarios, posts, bloqueios, categorias);
    }

    @Bean
    public ListarPostsDoUsuarioUseCase listarPostsDoUsuario(UsuarioGateway usuarios, PostGateway posts,
                                                            SeguirGateway seguires, BloqueioGateway bloqueios,
                                                            CategoriaGateway categorias) {
        return new ListarPostsDoUsuarioService(usuarios, posts, seguires, bloqueios, categorias);
    }

    @Bean
    public ComentarUseCase comentar(UsuarioGateway usuarios, PostGateway posts, BloqueioGateway bloqueios,
                                    ComentarioGateway comentarios, NotificacaoGateway notificacoes,
                                    TransacaoGateway transacao) {
        return new ComentarService(usuarios, posts, bloqueios, comentarios, notificacoes, transacao);
    }

    @Bean
    public IniciarConversaUseCase iniciarConversa(UsuarioGateway usuarios, BloqueioGateway bloqueios,
                                                  SeguirGateway seguires, ConversaGateway conversas) {
        return new IniciarConversaService(usuarios, bloqueios, seguires, conversas);
    }

    @Bean
    public ListarConversasUseCase listarConversas(UsuarioGateway usuarios, ConversaGateway conversas,
                                                  MensagemGateway mensagens) {
        return new ListarConversasService(usuarios, conversas, mensagens);
    }

    @Bean
    public MensagensUseCase mensagens(ConversaGateway conversas, MensagemGateway mensagens, BloqueioGateway bloqueios,
                                      ArmazenamentoGateway armazenamento, NotificacaoGateway notificacoes,
                                      TempoRealGateway tempoReal) {
        return new MensagensService(conversas, mensagens, bloqueios, armazenamento, notificacoes, tempoReal);
    }

    @Bean
    public VerPainelAdminUseCase verPainelAdmin(UsuarioGateway usuarios, InstituicaoGateway instituicoes) {
        return new VerPainelAdminService(usuarios, instituicoes);
    }
}
