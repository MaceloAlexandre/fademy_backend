package br.com.fademy.application.usecase;

import br.com.fademy.domain.conteudo.Post;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public interface PublicarPostUseCase {

    Post executar(Visualizador visualizador, String texto, String linkUrl, List<String> categoriaIds,
                  List<ArquivoEnviado> imagens);
}
