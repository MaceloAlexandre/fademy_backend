package br.com.fademy.application.usecase;

import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public interface ListarNotificacoesUseCase {

    Resultado executar(Visualizador visualizador, String cursor, String limite);

    record NotificacaoDetalhada(Notificacao notificacao, Usuario ator, StatusSeguir statusDoPedido) {
    }

    record Resultado(Pagina<NotificacaoDetalhada> pagina, long naoLidas) {
    }
}
