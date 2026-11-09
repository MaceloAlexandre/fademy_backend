package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public class MensagensComLimiteDeTamanho extends MensagensDecorator {

    private static final int MAXIMO_DE_CARACTERES = 500;

    public MensagensComLimiteDeTamanho(MensagensUseCase proximo) {
        super(proximo);
    }

    @Override
    public Mensagem enviar(Visualizador visualizador, String conversaId, String texto, List<ArquivoEnviado> imagens) {
        if (texto != null && texto.length() > MAXIMO_DE_CARACTERES) {
            throw new DadosInvalidosException("text is too long");
        }
        return super.enviar(visualizador, conversaId, texto, imagens);
    }
}
