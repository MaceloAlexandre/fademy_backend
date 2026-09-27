package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

/**
 * Marcar como lida e excluir uma notificação (SPEC RN36).
 */
public interface LerNotificacaoUseCase {

    void marcarComoLida(Visualizador visualizador, String notificacaoId);

    void excluir(Visualizador visualizador, String notificacaoId);
}
