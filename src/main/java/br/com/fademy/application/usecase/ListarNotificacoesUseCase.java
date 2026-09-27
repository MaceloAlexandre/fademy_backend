package br.com.fademy.application.usecase;

import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

/**
 * Notificações de quem pede, com o ator e o status atual dos pedidos de seguir (SPEC RN35).
 */
public interface ListarNotificacoesUseCase {

    Resultado executar(Visualizador visualizador, String cursor, String limite);

    /**
     * @param ator           quem gerou a notificação, lido do Postgres na hora (SPEC RN48)
     * @param statusDoPedido para follow_request: status atual do follow, ou null se ele não existe mais
     */
    record NotificacaoDetalhada(Notificacao notificacao, Usuario ator, StatusSeguir statusDoPedido) {
    }

    record Resultado(Pagina<NotificacaoDetalhada> pagina, long naoLidas) {
    }
}
