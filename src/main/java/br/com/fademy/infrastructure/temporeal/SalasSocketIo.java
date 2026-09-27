package br.com.fademy.infrastructure.temporeal;

import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.domain.excecao.NaoAutenticadoException;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cada conexão autenticada entra na sala com o id do usuário; o servidor só emite eventos
 * (SPEC RN43). A biblioteca não tem middleware de handshake: conexão com token ausente ou
 * inválido é encerrada logo depois de aberta, sem entrar em sala nenhuma.
 */
@Component
public class SalasSocketIo {

    private final SocketIoNamespace namespace;
    private final TokenGateway tokens;
    private final JsonMapper json;

    public SalasSocketIo(SocketIoServer servidor, TokenGateway tokens, JsonMapper json) {
        this.namespace = servidor.namespace("/");
        this.tokens = tokens;
        this.json = json;
        namespace.on("connection", args -> entrar((SocketIoSocket) args[0]));
    }

    /** Envia o evento, com os dados em JSON, para todas as conexões do usuário. */
    public void enviar(String usuarioId, String evento, Object dados) {
        namespace.broadcast(usuarioId, evento, new JSONTokener(json.writeValueAsString(dados)).nextValue());
    }

    private void entrar(SocketIoSocket socket) {
        try {
            socket.joinRoom(tokens.validar(token(socket)).id());
        } catch (NaoAutenticadoException e) {
            socket.disconnect(true);
        }
    }

    private static String token(SocketIoSocket socket) {
        return socket.getConnectData() instanceof JSONObject auth ? auth.optString("token", "") : "";
    }
}
