package br.com.fademy.application.gateway;

import br.com.fademy.domain.conversa.Mensagem;

import java.util.List;
import java.util.Optional;

public interface MensagemGateway {

    Mensagem criar(Mensagem mensagem);

    List<Mensagem> daConversa(String conversaId, String cursor, int quantidade);

    Optional<Mensagem> ultimaDe(String conversaId);
}
