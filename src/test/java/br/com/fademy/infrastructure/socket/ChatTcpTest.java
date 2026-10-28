package br.com.fademy.infrastructure.socket;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.usecase.IniciarConversaUseCase;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.SeguirUsuarioUseCase;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTcpTest extends IntegracaoTest {

    @Autowired
    ServidorTcp servidor;

    @Autowired
    Cenario cenario;

    @Autowired
    SeguirUsuarioUseCase seguir;

    @Autowired
    IniciarConversaUseCase iniciarConversa;

    @Autowired
    MensagensUseCase mensagens;

    private final List<Terminal> terminais = new ArrayList<>();

    @AfterEach
    void fecharTerminais() throws IOException {
        for (Terminal terminal : terminais) {
            terminal.socket.close();
        }
    }

    @Test
    void pedeEmailESenhaAoConectar() throws IOException {
        Terminal terminal = conectar();

        assertThat(terminal.ler()).isEqualTo("Informe seu e-mail:");
        terminal.enviar("alguem@teste.com");
        assertThat(terminal.ler()).isEqualTo("Informe sua senha:");
    }

    @Test
    void entraComEmailESenhaCertosEVeOsComandos() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO, cenario.catolica(), "Ana Lima", false);

        Terminal terminal = entrar(ana);

        assertThat(terminal.ler()).isEqualTo("Bem-vindo, Ana Lima!");
        assertThat(terminal.ler()).isEqualTo("Comandos: CONVERSAS | ENVIAR <número> <texto> | SAIR");
    }

    @Test
    void senhaErradaRecusaEEncerraAConexao() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Terminal terminal = conectar();

        terminal.ler();
        terminal.enviar(ana.email());
        terminal.ler();
        terminal.enviar("senha-errada");

        assertThat(terminal.ler()).isEqualTo("E-mail ou senha inválidos.");
        assertThat(terminal.ler()).isNull();
    }

    @Test
    void conversasListaONomeDoOutroMembro() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO, cenario.catolica(), "Bia Souza", false);
        conversaEntre(ana, bia);
        Terminal terminal = logado(ana);

        terminal.enviar("CONVERSAS");

        assertThat(terminal.ler()).isEqualTo("1) Bia Souza");
    }

    @Test
    void semConversasAvisa() throws IOException {
        Terminal terminal = logado(cenario.usuario(Papel.ALUNO));

        terminal.enviar("conversas");

        assertThat(terminal.ler()).isEqualTo("Nenhuma conversa.");
    }

    @Test
    void enviarGravaAMensagemNaConversa() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        String conversa = conversaEntre(ana, bia);
        Terminal terminal = logado(ana);
        terminal.enviar("CONVERSAS");
        terminal.ler();

        terminal.enviar("ENVIAR 1 Oi, Bia");

        assertThat(terminal.ler()).isEqualTo("Mensagem enviada.");
        assertThat(mensagens.listar(visualizador(bia), conversa, null, null).itens())
                .extracting(Mensagem::texto).containsExactly("Oi, Bia");
    }

    @Test
    void comandosErradosAvisamEMantemConectado() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        conversaEntre(ana, cenario.usuario(Papel.ALUNO));
        Terminal terminal = logado(ana);

        terminal.enviar("ENVIAR 9 Oi");
        assertThat(terminal.ler()).isEqualTo("Conversa inválida.");
        terminal.enviar("ENVIAR 1");
        assertThat(terminal.ler()).isEqualTo("Use: ENVIAR <número> <texto>");
        terminal.enviar("DANCAR");
        assertThat(terminal.ler()).isEqualTo("Comando inválido.");
        terminal.enviar("CONVERSAS");
        assertThat(terminal.ler()).startsWith("1) ");
    }

    @Test
    void recebeNaHoraAMensagemEnviadaPeloSite() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO, cenario.catolica(), "Ana Lima", false);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        String conversa = conversaEntre(ana, bia);
        Terminal terminalDaBia = logado(bia);

        mensagens.enviar(visualizador(ana), conversa, "chegou?", List.of());

        assertThat(terminalDaBia.ler()).isEqualTo("[Ana Lima] chegou?");
    }

    @Test
    void recebeNaHoraAMensagemEnviadaPorOutroTerminal() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO, cenario.catolica(), "Ana Lima", false);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        conversaEntre(ana, bia);
        Terminal terminalDaAna = logado(ana);
        Terminal terminalDaBia = logado(bia);

        terminalDaAna.enviar("ENVIAR 1 Oi pelo terminal");

        assertThat(terminalDaAna.ler()).isEqualTo("Mensagem enviada.");
        assertThat(terminalDaBia.ler()).isEqualTo("[Ana Lima] Oi pelo terminal");
    }

    @Test
    void sairEncerraAConexaoEOSiteContinuaEnviando() throws IOException {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        String conversa = conversaEntre(ana, bia);
        Terminal terminalDaBia = logado(bia);

        terminalDaBia.enviar("SAIR");

        assertThat(terminalDaBia.ler()).isEqualTo("Até logo!");
        assertThat(terminalDaBia.ler()).isNull();
        assertThat(mensagens.enviar(visualizador(ana), conversa, "ainda aí?", List.of()).id()).isNotNull();
    }

    private String conversaEntre(Usuario um, Usuario outro) {
        seguir.executar(visualizador(um), outro.id());
        seguir.executar(visualizador(outro), um.id());
        return iniciarConversa.executar(visualizador(um), outro.id()).conversa().id();
    }

    private static Visualizador visualizador(Usuario u) {
        return new Visualizador(u.id(), u.papel(), u.instituicaoId());
    }

    private Terminal logado(Usuario usuario) throws IOException {
        Terminal terminal = entrar(usuario);
        terminal.ler();
        terminal.ler();
        return terminal;
    }

    private Terminal entrar(Usuario usuario) throws IOException {
        Terminal terminal = conectar();
        terminal.ler();
        terminal.enviar(usuario.email());
        terminal.ler();
        terminal.enviar(Cenario.SENHA);
        return terminal;
    }

    private Terminal conectar() throws IOException {
        Terminal terminal = new Terminal(new Socket("localhost", servidor.porta()));
        terminais.add(terminal);
        return terminal;
    }

    private static final class Terminal {

        private final Socket socket;
        private final BufferedReader entrada;
        private final PrintWriter saida;

        Terminal(Socket socket) throws IOException {
            this.socket = socket;
            socket.setSoTimeout(5000);
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        }

        String ler() throws IOException {
            return entrada.readLine();
        }

        void enviar(String linha) {
            saida.println(linha);
        }
    }
}
