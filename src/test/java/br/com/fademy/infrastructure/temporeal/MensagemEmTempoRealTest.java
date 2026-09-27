package br.com.fademy.infrastructure.temporeal;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.usecase.IniciarConversaUseCase;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.SeguirUsuarioUseCase;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MensagemEmTempoRealTest extends IntegracaoTest {

    @LocalServerPort
    int porta;

    @Autowired
    Cenario cenario;

    @Autowired
    SeguirUsuarioUseCase seguir;

    @Autowired
    IniciarConversaUseCase iniciarConversa;

    @Autowired
    MensagensUseCase mensagens;

    @Test
    void destinatarioConectadoRecebeMessageNewNoFormatoDaApi() throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        seguir.executar(visualizador(ana), bia.id());
        seguir.executar(visualizador(bia), ana.id());
        String conversa = iniciarConversa.executar(visualizador(ana), bia.id()).conversa().id();

        BlockingQueue<Object> recebidas = new LinkedBlockingQueue<>();
        CountDownLatch conectou = new CountDownLatch(1);
        Socket socket = IO.socket(URI.create("http://localhost:" + porta), IO.Options.builder()
                .setAuth(Map.of("token", cenario.token(bia))).setReconnection(false).setForceNew(true).build());
        socket.on(Socket.EVENT_CONNECT, args -> conectou.countDown());
        socket.on("message:new", args -> recebidas.add(args[0]));
        socket.connect();
        assertThat(conectou.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(200);

        Mensagem enviada = mensagens.enviar(visualizador(ana), conversa, "chegou?", List.of());

        JSONObject evento = (JSONObject) recebidas.poll(5, TimeUnit.SECONDS);
        socket.close();
        assertThat(evento).isNotNull();
        assertThat(evento.getString("_id")).isEqualTo(enviada.id());
        assertThat(evento.getString("conversationId")).isEqualTo(conversa);
        assertThat(evento.getString("senderId")).isEqualTo(ana.id());
        assertThat(evento.getString("text")).isEqualTo("chegou?");
        assertThat(evento.getJSONArray("mediaUrls").length()).isZero();
        assertThat(evento.getString("createdAt")).endsWith("Z");
    }

    private static Visualizador visualizador(Usuario u) {
        return new Visualizador(u.id(), u.papel(), u.instituicaoId());
    }
}
