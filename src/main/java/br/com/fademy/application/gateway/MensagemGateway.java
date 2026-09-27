package br.com.fademy.application.gateway;

import br.com.fademy.domain.conversa.Mensagem;

import java.util.List;
import java.util.Optional;

/**
 * Mensagens no MongoDB.
 */
public interface MensagemGateway {

    /** Grava a mensagem e a devolve com id. */
    Mensagem criar(Mensagem mensagem);

    /**
     * Mensagens da conversa, da mais nova para a mais antiga, só as anteriores ao cursor.
     *
     * @throws br.com.fademy.domain.excecao.DadosInvalidosException se o cursor não é um id válido
     */
    List<Mensagem> daConversa(String conversaId, String cursor, int quantidade);

    Optional<Mensagem> ultimaDe(String conversaId);
}
