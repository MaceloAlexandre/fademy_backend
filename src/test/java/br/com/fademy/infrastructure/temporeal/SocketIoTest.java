package br.com.fademy.infrastructure.temporeal;

import br.com.fademy.Cenario;
import br.com.fademy.IntegracaoTest;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cliente Socket.IO (Java, protocolo v4, o mesmo do socket.io-client do React) contra o servidor
 * de verdade na porta da API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SocketIoTest extends IntegracaoTest {

    @LocalServerPort
    int porta;

    @Autowired
    Cenario cenario;

    @Autowired
    SalasSocketIo salas;

    private final List<Socket> abertos = new ArrayList<>();

    @AfterEach
    void fechar() {
        abertos.forEach(Socket::close);
    }

    @ParameterizedTest
    @ValueSource(strings = {"polling", "websocket"})
    void conexaoAutenticadaRecebeOsEventosDoUsuario(String transporte) throws Exception {
        Usuario ana = cenario.usuario(Papel.ALUNO);
        Usuario bia = cenario.usuario(Papel.ALUNO);
        BlockingQueue<Object> daAna = new LinkedBlockingQueue<>();
        BlockingQueue<Object> daBia = new LinkedBlockingQueue<>();
        conectar(cenario.token(ana), transporte, daAna);
        conectar(cenario.token(bia), transporte, daBia);

        salas.enviar(ana.id(), "message:new", Map.of("text", "oi, Ana"));

        Object recebido = daAna.poll(5, TimeUnit.SECONDS);
        assertThat(recebido).isInstanceOf(JSONObject.class);
        assertThat(((JSONObject) recebido).getString("text")).isEqualTo("oi, Ana");
        assertThat(daBia.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "token-invalido"})
    void tokenInvalidoEhDesconectado(String token) throws Exception {
        CountDownLatch desconectou = new CountDownLatch(1);
        Socket socket = socket(token, "polling");
        socket.on(Socket.EVENT_DISCONNECT, args -> desconectou.countDown());
        socket.connect();

        assertThat(desconectou.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void corsDoPollingLiberaOFrontend() throws Exception {
        HttpResponse<String> resposta = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/socket.io/?EIO=4&transport=polling"))
                .header("Origin", "http://localhost:5173").build(), HttpResponse.BodyHandlers.ofString());

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
    }

    @Test
    void aApiRestContinuaNaMesmaPorta() throws Exception {
        HttpResponse<String> resposta = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + porta + "/health")).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(resposta.statusCode()).isEqualTo(200);
    }

    private void conectar(String token, String transporte, BlockingQueue<Object> recebidos) throws Exception {
        CountDownLatch conectou = new CountDownLatch(1);
        Socket socket = socket(token, transporte);
        socket.on(Socket.EVENT_CONNECT, args -> conectou.countDown());
        socket.on("message:new", args -> recebidos.add(args[0]));
        socket.connect();
        assertThat(conectou.await(5, TimeUnit.SECONDS)).as("conectou").isTrue();
        // A entrada na sala acontece logo depois do connect; espera ela ser processada.
        Thread.sleep(200);
    }

    private Socket socket(String token, String transporte) {
        Socket socket = IO.socket(URI.create("http://localhost:" + porta), IO.Options.builder()
                .setAuth(Map.of("token", token))
                .setTransports(new String[]{transporte})
                .setReconnection(false)
                .setForceNew(true)
                .build());
        abertos.add(socket);
        return socket;
    }
}
