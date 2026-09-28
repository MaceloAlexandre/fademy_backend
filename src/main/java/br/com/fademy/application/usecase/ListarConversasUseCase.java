package br.com.fademy.application.usecase;

import br.com.fademy.domain.conversa.Conversa;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public interface ListarConversasUseCase {

    List<ConversaDetalhada> conversas(Visualizador visualizador);

    List<Usuario> contatos(Visualizador visualizador);

    record ConversaDetalhada(Conversa conversa, List<Usuario> membros, Mensagem ultimaMensagem) {
    }
}
