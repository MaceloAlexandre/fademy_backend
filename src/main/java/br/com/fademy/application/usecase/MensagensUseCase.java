package br.com.fademy.application.usecase;

import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public interface MensagensUseCase {

    Mensagem enviar(Visualizador visualizador, String conversaId, String texto, List<ArquivoEnviado> imagens);

    Pagina<Mensagem> listar(Visualizador visualizador, String conversaId, String cursor, String limite);
}
