package br.com.fademy.infrastructure.config;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.AceitarPedidoUseCase;
import br.com.fademy.application.usecase.ComentarUseCase;
import br.com.fademy.application.usecase.EditarPerfilUseCase;
import br.com.fademy.application.usecase.EditarPerfilUseCase.EdicaoDePerfil;
import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import br.com.fademy.application.usecase.EnviarAvatarUseCase.ImagemEnviada;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Dados para a apresentação (perfil "demo"). Tudo passa pelos casos de uso, na ordem de um uso
 * real: o admin gera convites, cada pessoa envia a foto e se cadastra, segue, publica, comenta e
 * conversa. Se a professora de demonstração já existe, não faz nada.
 */
@Component
@Profile("demo")
@Order(2)
public class DadosDeDemonstracao implements ApplicationRunner {

    public static final String SENHA = "demo1234";
    static final String EMAIL_DA_PROFESSORA = "helena@demo.fademy";

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
        if (usuarios.buscarPorEmail(EMAIL_DA_PROFESSORA).isPresent()) {
            return;
        }
        Visualizador admin = visualizador(usuarios.buscarPorEmail(emailDoAdmin).orElseThrow());

        Usuario helena = pessoa(admin, "professor", "Profa. Helena Duarte", EMAIL_DA_PROFESSORA,
                new Color(0x1B4F81));
        Usuario marcos = pessoa(admin, "coordenador", "Coord. Marcos Lima", "marcos@demo.fademy",
                new Color(0x7B5EA7));
        Usuario ana = pessoa(admin, "aluno", "Ana Souza", "ana@demo.fademy", new Color(0xE07A5F));
        Usuario bruno = pessoa(admin, "aluno", "Bruno Costa", "bruno@demo.fademy", new Color(0x3D405B));
        Usuario carla = pessoa(admin, "aluno", "Carla Nunes", "carla@demo.fademy", new Color(0x81B29A));
        editarPerfil.executar(visualizador(bruno), new EdicaoDePerfil(true, true, false, null, null));

        seguirUm(ana, helena);
        seguirUm(helena, ana);
        seguirUm(ana, marcos);
        seguirUm(marcos, helena);
        seguirUm(helena, marcos);
        seguirUm(carla, helena);
        seguirUm(helena, carla);
        seguirUm(carla, ana);
        aceitarPedido.executar(visualizador(bruno), seguirUm(ana, bruno).id());
        seguirUm(carla, bruno);

        Post aula = publicar.executar(visualizador(helena),
                "Slides da aula de hoje: padrões Singleton e Adapter. Tragam dúvidas para a próxima aula!",
                "https://refactoring.guru/pt-br/design-patterns/singleton",
                List.of(categoria("aulas"), categoria("ciencia-da-computacao")), List.of());
        publicar.executar(visualizador(marcos),
                "A semana acadêmica começa segunda-feira. Inscrições abertas na coordenação.",
                null, List.of(categoria("dia-a-dia")), List.of());
        publicar.executar(visualizador(ana),
                "Nosso projeto de POO: a Fademy reescrita em Java com Clean Architecture, Postgres e MongoDB.",
                null, List.of(categoria("projetos"), categoria("ciencia-da-computacao")), List.of());
        publicar.executar(visualizador(carla),
                "Quando o professor diz que o getInstance() sem synchronized é seguro... e explica por quê.",
                null, List.of(categoria("meme")), List.of());
        comentar.comentar(visualizador(ana), aula.id(),
                "Obrigada, professora! O exemplo do DatabaseManager ajudou muito.");
        comentar.comentar(visualizador(carla), aula.id(), "Vai cair na prova?");

        String conversa = iniciarConversa.executar(visualizador(ana), helena.id()).conversa().id();
        mensagens.enviar(visualizador(ana), conversa,
                "Professora, posso tirar uma dúvida sobre o trabalho?", List.of());
        mensagens.enviar(visualizador(helena), conversa, "Claro, Ana! Pode mandar.", List.of());
    }

    /** Uma pessoa entrando como na vida real: convite do admin, foto e cadastro. */
    private Usuario pessoa(Visualizador admin, String papel, String nome, String email, Color cor) {
        String convite = gerarConvite.executar(admin, papel, 1, null).token();
        ImagemEnviada foto = enviarAvatar.executar(avatar(cor), "image/png");
        return registrar.executar(new Cadastro(convite, nome, email, SENHA, foto.url(), foto.chave()));
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

    private static byte[] avatar(Color cor) {
        BufferedImage imagem = new BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB);
        var grafico = imagem.createGraphics();
        grafico.setColor(cor);
        grafico.fillRect(0, 0, 96, 96);
        grafico.dispose();
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
