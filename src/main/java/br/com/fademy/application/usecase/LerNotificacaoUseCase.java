package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Visualizador;

public interface LerNotificacaoUseCase {

    void marcarComoLida(Visualizador visualizador, String notificacaoId);

    void excluir(Visualizador visualizador, String notificacaoId);
}
