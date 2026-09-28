package br.com.fademy.application.usecase;

import br.com.fademy.application.gateway.ConversaGateway.Resultado;
import br.com.fademy.domain.usuario.Visualizador;

public interface IniciarConversaUseCase {

    Resultado executar(Visualizador visualizador, String outroId);
}
