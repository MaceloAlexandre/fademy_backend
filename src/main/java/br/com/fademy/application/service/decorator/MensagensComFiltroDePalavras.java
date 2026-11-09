package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;
import java.util.regex.Pattern;

public class MensagensComFiltroDePalavras extends MensagensDecorator {

    private static final Pattern PROIBIDAS = Pattern.compile("\\b(porcaria|droga|idiota|otário)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    public MensagensComFiltroDePalavras(MensagensUseCase proximo) {
        super(proximo);
    }

    @Override
    public Mensagem enviar(Visualizador visualizador, String conversaId, String texto, List<ArquivoEnviado> imagens) {
        return super.enviar(visualizador, conversaId, censurar(texto), imagens);
    }

    private static String censurar(String texto) {
        if (texto == null) {
            return null;
        }
        return PROIBIDAS.matcher(texto).replaceAll(palavra -> "*".repeat(palavra.group().length()));
    }
}
