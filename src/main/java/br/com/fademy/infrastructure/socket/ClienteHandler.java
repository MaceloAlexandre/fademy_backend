package br.com.fademy.infrastructure.socket;

import br.com.fademy.application.usecase.ListarConversasUseCase.ConversaDetalhada;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Visualizador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClienteHandler implements Runnable {

    private final Socket socket;
    private final ServidorTcp servidor;
    private BufferedReader entrada;
    private PrintWriter saida;
    private Visualizador visualizador;
    private String nome;
    private List<ConversaDetalhada> conversas;

    public ClienteHandler(Socket socket, ServidorTcp servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            enviarMensagem("Informe seu e-mail:");
            String email = entrada.readLine();
            enviarMensagem("Informe sua senha:");
            String senha = entrada.readLine();

            if (!servidor.autenticar(this, email, senha)) {
                enviarMensagem("E-mail ou senha inválidos.");
                return;
            }

            servidor.adicionarCliente(this);

            enviarMensagem("Bem-vindo, " + nome + "!");
            enviarMensagem("Comandos: CONVERSAS | ENVIAR <número> <texto> | SAIR");

            String mensagem;

            while ((mensagem = entrada.readLine()) != null) {
                if (mensagem.trim().equalsIgnoreCase("SAIR")) {
                    enviarMensagem("Até logo!");
                    break;
                }

                servidor.processarMensagem(this, mensagem);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado.");
        } finally {
            servidor.removerCliente(this);

            try {
                socket.close();
            } catch (IOException ignorada) {
            }
        }
    }

    void entrou(Visualizador visualizador, String nome) {
        this.visualizador = visualizador;
        this.nome = nome;
    }

    void guardarConversas(List<ConversaDetalhada> conversas) {
        this.conversas = conversas;
    }

    void novaMensagem(Mensagem mensagem) {
        servidor.entregarNovaMensagem(this, mensagem);
    }

    public synchronized void enviarMensagem(String mensagem) {
        if (saida != null) {
            saida.println(mensagem);
        }
    }

    public Visualizador visualizador() {
        return visualizador;
    }

    public String nome() {
        return nome;
    }

    List<ConversaDetalhada> conversas() {
        return conversas;
    }
}
