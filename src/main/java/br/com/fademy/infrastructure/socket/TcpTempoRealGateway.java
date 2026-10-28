package br.com.fademy.infrastructure.socket;

import br.com.fademy.application.gateway.TempoRealGateway;
import br.com.fademy.domain.conversa.Mensagem;
import org.springframework.stereotype.Component;

@Component
public class TcpTempoRealGateway implements TempoRealGateway {

    private final ClientesConectados clientes;

    public TcpTempoRealGateway(ClientesConectados clientes) {
        this.clientes = clientes;
    }

    @Override
    public void novaMensagem(String destinatarioId, Mensagem mensagem) {
        clientes.enviarPara(destinatarioId, mensagem);
    }
}
