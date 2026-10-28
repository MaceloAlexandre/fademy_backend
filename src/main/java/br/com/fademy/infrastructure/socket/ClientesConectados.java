package br.com.fademy.infrastructure.socket;

import br.com.fademy.domain.conversa.Mensagem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ClientesConectados {

    private final List<ClienteHandler> clientes = new CopyOnWriteArrayList<>();

    public void adicionar(ClienteHandler cliente) {
        clientes.add(cliente);
    }

    public boolean remover(ClienteHandler cliente) {
        return clientes.remove(cliente);
    }

    public void enviarPara(String usuarioId, Mensagem mensagem) {
        for (ClienteHandler cliente : clientes) {
            if (cliente.visualizador().id().equals(usuarioId)) {
                cliente.novaMensagem(mensagem);
            }
        }
    }
}
