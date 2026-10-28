package br.com.fademy.infrastructure.temporeal;

import br.com.fademy.application.gateway.TempoRealGateway;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.infrastructure.socket.TcpTempoRealGateway;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class TempoRealEmTodosOsCanais implements TempoRealGateway {

    private final List<TempoRealGateway> canais;

    public TempoRealEmTodosOsCanais(SocketIoTempoRealGateway socketIo, TcpTempoRealGateway tcp) {
        this.canais = List.of(socketIo, tcp);
    }

    @Override
    public void novaMensagem(String destinatarioId, Mensagem mensagem) {
        for (TempoRealGateway canal : canais) {
            canal.novaMensagem(destinatarioId, mensagem);
        }
    }
}
