package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public abstract class MensagensDecorator implements MensagensUseCase {

    private final MensagensUseCase proximo;

    protected MensagensDecorator(MensagensUseCase proximo) {
        this.proximo = proximo;
    }

    @Override
    public Mensagem enviar(Visualizador visualizador, String conversaId, String texto, List<ArquivoEnviado> imagens) {
        return proximo.enviar(visualizador, conversaId, texto, imagens);
    }

    @Override
    public Pagina<Mensagem> listar(Visualizador visualizador, String conversaId, String cursor, String limite) {
        return proximo.listar(visualizador, conversaId, cursor, limite);
    }
}
