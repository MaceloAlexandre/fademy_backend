package br.com.fademy.infrastructure.temporeal;

import br.com.fademy.application.gateway.TempoRealGateway;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.infrastructure.web.EnderecoDasImagens;
import br.com.fademy.infrastructure.web.MensagemResponse;
import org.springframework.stereotype.Component;

/**
 * Evento message:new com a mensagem no mesmo formato da API REST (SPEC RN43).
 */
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
