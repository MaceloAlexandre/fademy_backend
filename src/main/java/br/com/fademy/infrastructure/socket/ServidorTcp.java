package br.com.fademy.infrastructure.socket;

import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.usecase.AutenticarUseCase;
import br.com.fademy.application.usecase.ListarConversasUseCase;
import br.com.fademy.application.usecase.ListarConversasUseCase.ConversaDetalhada;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.VerPerfilUseCase;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

@Component
public class ServidorTcp implements SmartLifecycle {

    private final int portaConfigurada;
    private final ClientesConectados clientes;
    private final AutenticarUseCase autenticar;
    private final TokenGateway tokens;
    private final VerPerfilUseCase perfil;
    private final ListarConversasUseCase conversas;
    private final MensagensUseCase mensagens;
    private ServerSocket serverSocket;

    public ServidorTcp(@Value("${fademy.tcp.porta}") int portaConfigurada, ClientesConectados clientes,
                       AutenticarUseCase autenticar, TokenGateway tokens, VerPerfilUseCase perfil,
                       ListarConversasUseCase conversas, MensagensUseCase mensagens) {
        this.portaConfigurada = portaConfigurada;
        this.clientes = clientes;
        this.autenticar = autenticar;
        this.tokens = tokens;
        this.perfil = perfil;
        this.conversas = conversas;
        this.mensagens = mensagens;
    }

    @Override
    public void start() {
        try {
            serverSocket = new ServerSocket(portaConfigurada);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível abrir a porta TCP " + portaConfigurada, e);
        }
        System.out.println("Servidor TCP iniciado na porta " + porta() + ".");

        Thread aceitador = new Thread(this::aceitarConexoes, "servidor-tcp");
        aceitador.setDaemon(true);
        aceitador.start();
    }

    private void aceitarConexoes() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();

                ClienteHandler handler = new ClienteHandler(socket, this);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Servidor TCP encerrado.");
        }
    }

    public boolean autenticar(ClienteHandler cliente, String email, String senha) {
        try {
            Visualizador visualizador = tokens.validar(autenticar.executar(email, senha));
            String nome = perfil.executar(visualizador, visualizador.id()).usuario().nome();
            cliente.entrou(visualizador, nome);
            return true;
        } catch (NaoAutenticadoException | DadosInvalidosException e) {
            return false;
        }
    }

    public void adicionarCliente(ClienteHandler cliente) {
        clientes.adicionar(cliente);
        System.out.println("Conectado pelo terminal: " + cliente.nome());
    }

    public void removerCliente(ClienteHandler cliente) {
        if (clientes.remover(cliente)) {
            System.out.println("Desconectado do terminal: " + cliente.nome());
        }
    }

    public void processarMensagem(ClienteHandler cliente, String mensagem) {
        String[] partes = mensagem.trim().split("\\s+", 3);

        switch (partes[0].toUpperCase()) {
            case "CONVERSAS" -> listarConversas(cliente);
            case "ENVIAR" -> enviar(cliente, partes);
            default -> cliente.enviarMensagem("Comando inválido.");
        }
    }

    private void listarConversas(ClienteHandler cliente) {
        List<ConversaDetalhada> minhas = conversas.conversas(cliente.visualizador());
        cliente.guardarConversas(minhas);

        if (minhas.isEmpty()) {
            cliente.enviarMensagem("Nenhuma conversa.");
            return;
        }

        int numero = 1;

        for (ConversaDetalhada conversa : minhas) {
            cliente.enviarMensagem(numero++ + ") " + nomeDoOutroMembro(conversa, cliente.visualizador().id()));
        }
    }

    private void enviar(ClienteHandler cliente, String[] partes) {
        if (partes.length < 3) {
            cliente.enviarMensagem("Use: ENVIAR <número> <texto>");
            return;
        }

        if (cliente.conversas() == null) {
            cliente.guardarConversas(conversas.conversas(cliente.visualizador()));
        }

        ConversaDetalhada conversa = escolhida(cliente.conversas(), partes[1]);

        if (conversa == null) {
            cliente.enviarMensagem("Conversa inválida.");
            return;
        }

        try {
            mensagens.enviar(cliente.visualizador(), conversa.conversa().id(), partes[2], List.of());
            cliente.enviarMensagem("Mensagem enviada.");
        } catch (AcessoNegadoException | DadosInvalidosException | RecursoNaoEncontradoException e) {
            cliente.enviarMensagem("Não foi possível enviar: " + e.getMessage());
        }
    }

    public void entregarNovaMensagem(ClienteHandler cliente, Mensagem mensagem) {
        String remetente = conversas.conversas(cliente.visualizador()).stream()
                .filter(conversa -> conversa.conversa().id().equals(mensagem.conversaId()))
                .flatMap(conversa -> conversa.membros().stream())
                .filter(membro -> membro.id().equals(mensagem.remetenteId()))
                .map(Usuario::nome)
                .findFirst()
                .orElse("(sem nome)");

        cliente.enviarMensagem("[" + remetente + "] " + (mensagem.texto() != null ? mensagem.texto() : "(imagem)"));
    }

    private static ConversaDetalhada escolhida(List<ConversaDetalhada> lista, String numero) {
        try {
            return lista.get(Integer.parseInt(numero) - 1);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static String nomeDoOutroMembro(ConversaDetalhada conversa, String meuId) {
        return conversa.membros().stream()
                .filter(membro -> !membro.id().equals(meuId))
                .map(Usuario::nome)
                .findFirst()
                .orElse("(sem nome)");
    }

    @Override
    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException ignorada) {
        }
    }

    @Override
    public boolean isRunning() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    public int porta() {
        return serverSocket.getLocalPort();
    }
}
