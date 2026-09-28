package br.com.fademy.infrastructure.temporeal;

import br.com.fademy.application.gateway.TempoRealGateway;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.infrastructure.web.EnderecoDasImagens;
import br.com.fademy.infrastructure.web.MensagemResponse;
import org.springframework.stereotype.Component;

@Component
public class SocketIoTempoRealGateway implements TempoRealGateway {

    private final SalasSocketIo salas;
    private final EnderecoDasImagens imagens;

    public SocketIoTempoRealGateway(SalasSocketIo salas, EnderecoDasImagens imagens) {
        this.salas = salas;
        this.imagens = imagens;
    }

    @Override
    public void novaMensagem(String destinatarioId, Mensagem mensagem) {
        salas.enviar(destinatarioId, "message:new", MensagemResponse.de(mensagem, imagens));
    }
}
