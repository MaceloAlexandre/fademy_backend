package br.com.fademy.application.usecase;

import br.com.fademy.application.gateway.ConversaGateway.Resultado;
import br.com.fademy.domain.usuario.Visualizador;

/**
 * Abre (ou reabre) a conversa 1:1 com outro usuário (SPEC RN38).
 */
public interface IniciarConversaUseCase {

    Resultado executar(Visualizador visualizador, String outroId);
}
