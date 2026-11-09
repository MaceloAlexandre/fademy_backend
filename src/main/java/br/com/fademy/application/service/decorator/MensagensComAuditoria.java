package br.com.fademy.application.service.decorator;

import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;
import java.util.function.Consumer;

public class MensagensComAuditoria extends MensagensDecorator {

    private final Consumer<String> saida;

    public MensagensComAuditoria(MensagensUseCase proximo, Consumer<String> saida) {
        super(proximo);
        this.saida = saida;
    }

    @Override
    public Mensagem enviar(Visualizador visualizador, String conversaId, String texto, List<ArquivoEnviado> imagens) {
        long inicio = System.nanoTime();
        String resultado = "recusado";
        try {
            Mensagem mensagem = super.enviar(visualizador, conversaId, texto, imagens);
            resultado = "ok";
            return mensagem;
        } finally {
            long milissegundos = (System.nanoTime() - inicio) / 1_000_000;
            saida.accept("[auditoria] remetente=%s conversa=%s resultado=%s tempo=%dms"
                    .formatted(visualizador.id(), conversaId, resultado, milissegundos));
        }
    }
}
