package br.com.fademy.infrastructure.config;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.AceitarPedidoUseCase;
import br.com.fademy.application.usecase.ComentarUseCase;
import br.com.fademy.application.usecase.EditarPerfilUseCase;
import br.com.fademy.application.usecase.EditarPerfilUseCase.EdicaoDePerfil;
import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import br.com.fademy.application.usecase.GerarConviteUseCase;
import br.com.fademy.application.usecase.IniciarConversaUseCase;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.PublicarPostUseCase;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase.Cadastro;
import br.com.fademy.application.usecase.SeguirUsuarioUseCase;
import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import br.com.fademy.infrastructure.armazenamento.AvatarDeIniciais;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;

@Component
@Profile("demo")
@Order(2)
public class DadosDeDemonstracao implements ApplicationRunner {

    public static final String SENHA = "demo1234";
    static final String EMAIL_DO_PROFESSOR = "erlon@demo.fademy";
    static final String EMAIL_DO_GUSTAVO = "gustavo@demo.fademy";

    private final UsuarioGateway usuarios;
    private final CategoriaGateway categorias;
    private final GerarConviteUseCase gerarConvite;
    private final EnviarAvatarUseCase enviarAvatar;
    private final RegistrarViaConviteUseCase registrar;
    private final EditarPerfilUseCase editarPerfil;
    private final SeguirUsuarioUseCase seguir;
    private final AceitarPedidoUseCase aceitarPedido;
    private final PublicarPostUseCase publicar;
    private final ComentarUseCase comentar;
    private final IniciarConversaUseCase iniciarConversa;
    private final MensagensUseCase mensagens;
    private final String emailDoAdmin;

    public DadosDeDemonstracao(UsuarioGateway usuarios, CategoriaGateway categorias, GerarConviteUseCase gerarConvite,
                               EnviarAvatarUseCase enviarAvatar, RegistrarViaConviteUseCase registrar,
                               EditarPerfilUseCase editarPerfil, SeguirUsuarioUseCase seguir,
                               AceitarPedidoUseCase aceitarPedido, PublicarPostUseCase publicar,
                               ComentarUseCase comentar, IniciarConversaUseCase iniciarConversa,
                               MensagensUseCase mensagens, @Value("${fademy.admin.email}") String emailDoAdmin) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.gerarConvite = gerarConvite;
        this.enviarAvatar = enviarAvatar;
        this.registrar = registrar;
        this.editarPerfil = editarPerfil;
        this.seguir = seguir;
        this.aceitarPedido = aceitarPedido;
        this.publicar = publicar;
        this.comentar = comentar;
        this.iniciarConversa = iniciarConversa;
        this.mensagens = mensagens;
        this.emailDoAdmin = emailDoAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        Visualizador admin = visualizador(usuarios.buscarPorEmail(emailDoAdmin).orElseThrow());
        if (usuarios.buscarPorEmail(EMAIL_DO_PROFESSOR).isEmpty()) {
            criarTurma(admin);
        }
        if (usuarios.buscarPorEmail(EMAIL_DO_GUSTAVO).isEmpty()) {
            criarDuplaDoChatPeloTerminal(admin);
        }
    }

    private void criarTurma(Visualizador admin) {
        Usuario erlon = pessoa(admin, "professor", "Professor Erlon Dantas", EMAIL_DO_PROFESSOR,
                new Color(0x1B4F81));
        Usuario rene = pessoa(admin, "coordenador", "Coord. Renê Gadelha", "rene@demo.fademy",
                new Color(0x7B5EA7));
        Usuario fabricio = pessoa(admin, "professor", "Professor Fabricio Natam", "fabricio@demo.fademy",
                new Color(0x2A9D8F));
        Usuario erika = pessoa(admin, "professor", "Professora Erika", "erika@demo.fademy", new Color(0x81B29A));
        Usuario wacelys = pessoa(admin, "aluno", "Wacelys", "wacelys@demo.fademy", new Color(0xE07A5F));
        Usuario artur = pessoa(admin, "aluno", "Artur", "artur@demo.fademy", new Color(0x3D405B));
        editarPerfil.executar(visualizador(artur), new EdicaoDePerfil(true, true, false, null, null));

        seguirUm(wacelys, erlon);
        seguirUm(erlon, wacelys);
        seguirUm(wacelys, rene);
        seguirUm(rene, erlon);
        seguirUm(erlon, rene);
        seguirUm(erika, erlon);
        seguirUm(erlon, erika);
        seguirUm(erika, wacelys);
        seguirUm(fabricio, erlon);
        seguirUm(erlon, fabricio);
        aceitarPedido.executar(visualizador(artur), seguirUm(wacelys, artur).id());
        seguirUm(erika, artur);

        Post aula = publicar.executar(visualizador(erlon),
                "Slides da aula de hoje: padrões Singleton e Adapter. Tragam dúvidas para a próxima aula!",
                "https://refactoring.guru/pt-br/design-patterns/singleton",
                List.of(categoria("aulas"), categoria("ciencia-da-computacao")), List.of());
        publicar.executar(visualizador(rene),
                "A semana acadêmica começa segunda-feira. Inscrições abertas na coordenação.",
                null, List.of(categoria("dia-a-dia")), List.of());
        publicar.executar(visualizador(fabricio),
                "Plantão de dúvidas de Banco de Dados na quinta-feira, às 19h.",
                null, List.of(categoria("aulas")), List.of());
        publicar.executar(visualizador(wacelys),
                "Nosso projeto de POO: a Fademy reescrita em Java com Clean Architecture, Postgres e MongoDB.",
                null, List.of(categoria("projetos"), categoria("ciencia-da-computacao")), List.of());
        publicar.executar(visualizador(artur),
                "Quando o professor diz que o getInstance() sem synchronized é seguro... e explica por quê.",
                null, List.of(categoria("meme")), List.of());
        comentar.comentar(visualizador(wacelys), aula.id(),
                "Obrigado, professor! O exemplo do DatabaseManager ajudou muito.");
        comentar.comentar(visualizador(erika), aula.id(), "Ótimo material, vou indicar para a minha turma.");

        String conversa = iniciarConversa.executar(visualizador(wacelys), erlon.id()).conversa().id();
        mensagens.enviar(visualizador(wacelys), conversa,
                "Professor, posso tirar uma dúvida sobre o trabalho?", List.of());
        mensagens.enviar(visualizador(erlon), conversa, "Claro, Wacelys! Pode mandar.", List.of());
    }

    private void criarDuplaDoChatPeloTerminal(Visualizador admin) {
        Usuario gustavo = pessoa(admin, "professor", "Gustavo", EMAIL_DO_GUSTAVO, new Color(0xF2A541));
        Usuario lucas = pessoa(admin, "aluno", "Lucas Moreira", "lucas@demo.fademy", new Color(0x4C6EF5));
        seguirUm(gustavo, lucas);
        seguirUm(lucas, gustavo);

        String conversa = iniciarConversa.executar(visualizador(lucas), gustavo.id()).conversa().id();
        mensagens.enviar(visualizador(lucas), conversa, "Oi, Gustavo! Bora testar o chat pelo terminal?", List.of());
    }

    private Usuario pessoa(Visualizador admin, String papel, String nome, String email, Color cor) {
        String convite = gerarConvite.executar(admin, papel, 1, null).token();
        String foto = enviarAvatar.executar(AvatarDeIniciais.png(nome, cor), "image/png");
        return registrar.executar(new Cadastro(convite, nome, email, SENHA, foto, foto));
    }

    private Seguir seguirUm(Usuario quem, Usuario alvo) {
        return seguir.executar(visualizador(quem), alvo.id());
    }

    private String categoria(String slug) {
        return categorias.listar().stream().filter(c -> c.slug().equals(slug)).map(Categoria::id)
                .findFirst().orElseThrow();
    }

    private static Visualizador visualizador(Usuario u) {
        return new Visualizador(u.id(), u.papel(), u.instituicaoId());
    }
}
