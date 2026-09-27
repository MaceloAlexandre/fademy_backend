package br.com.fademy.application.usecase;

import br.com.fademy.domain.conversa.Conversa;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

/**
 * Conversas de quem pede e contatos com quem pode conversar (SPEC RN39, RN40).
 */
public interface ListarConversasUseCase {

    List<ConversaDetalhada> conversas(Visualizador visualizador);

    List<Usuario> contatos(Visualizador visualizador);

    /**
     * @param ultimaMensagem nula quando a conversa ainda não tem mensagens
     */
    record ConversaDetalhada(Conversa conversa, List<Usuario> membros, Mensagem ultimaMensagem) {
    }
}
