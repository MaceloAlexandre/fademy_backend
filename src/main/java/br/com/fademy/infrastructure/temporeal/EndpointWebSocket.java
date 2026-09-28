package br.com.fademy.infrastructure.temporeal;

import io.socket.engineio.server.EngineIoServer;
import io.socket.engineio.server.EngineIoWebSocket;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EndpointWebSocket extends Endpoint {

    private static final String CABECALHOS = "cabecalhos";

    private final EngineIoServer engineIoServer;
    private SessaoEngineIo sessao;

    EndpointWebSocket(EngineIoServer engineIoServer) {
        this.engineIoServer = engineIoServer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onOpen(Session session, EndpointConfig config) {
        sessao = new SessaoEngineIo(session, (Map<String, List<String>>) config.getUserProperties().get(CABECALHOS));
        session.addMessageHandler(String.class, (MessageHandler.Whole<String>) texto -> sessao.emit("message", texto));
        session.addMessageHandler(ByteBuffer.class,
                (MessageHandler.Whole<ByteBuffer>) dados -> sessao.emit("message", (Object) toArray(dados)));
        engineIoServer.handleWebSocket(sessao);
    }

    @Override
    public void onClose(Session session, CloseReason motivo) {
        sessao.emit("close");
    }

    @Override
    public void onError(Session session, Throwable erro) {
        sessao.emit("error", "write error", erro.getMessage());
    }

    private static byte[] toArray(ByteBuffer dados) {
        byte[] bytes = new byte[dados.remaining()];
        dados.get(bytes);
        return bytes;
    }

    private static final class SessaoEngineIo extends EngineIoWebSocket {

        private final Session session;
        private final Map<String, List<String>> cabecalhos;

        SessaoEngineIo(Session session, Map<String, List<String>> cabecalhos) {
            this.session = session;
            this.cabecalhos = cabecalhos;
        }

        @Override
        public Map<String, String> getQuery() {
            Map<String, String> query = new HashMap<>();
            String texto = session.getQueryString();
            if (texto != null) {
                Arrays.stream(texto.split("&")).map(par -> par.split("=", 2))
                        .forEach(par -> query.put(par[0], par.length > 1 ? par[1] : ""));
            }
            return query;
        }

        @Override
        public Map<String, List<String>> getConnectionHeaders() {
            return cabecalhos;
        }

        @Override
        public void write(String mensagem) throws IOException {
            session.getBasicRemote().sendText(mensagem);
        }

        @Override
        public void write(byte[] mensagem) throws IOException {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(mensagem));
        }

        @Override
        public void close() {
            try {
                session.close();
            } catch (IOException ignorada) {
            }
        }
    }

    static final class Configurador extends ServerEndpointConfig.Configurator {

        private final EngineIoServer engineIoServer;

        Configurador(EngineIoServer engineIoServer) {
            this.engineIoServer = engineIoServer;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            config.getUserProperties().put(CABECALHOS, request.getHeaders());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getEndpointInstance(Class<T> classe) {
            return (T) new EndpointWebSocket(engineIoServer);
        }
    }
}
