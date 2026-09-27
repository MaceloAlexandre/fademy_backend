package br.com.fademy.domain.conversa;

import br.com.fademy.domain.excecao.DadosInvalidosException;

import java.time.Instant;
import java.util.List;

/**
 * Mensagem de uma conversa: texto, imagens ou os dois (SPEC RN41).
 *
 * @param texto nulo quando a mensagem só tem imagens
 */
public record Mensagem(String id, String conversaId, String remetenteId, String texto, List<String> midias,
                       Instant criadaEm) {

    /** Mensagem nova, ainda sem as URLs das imagens; precisa de texto ou de pelo menos uma imagem. */
    public static Mensagem escrever(String conversaId, String remetenteId, String texto, int quantidadeDeImagens,
                                    Instant agora) {
        boolean temTexto = texto != null && !texto.isBlank();
        if (!temTexto && quantidadeDeImagens == 0) {
            throw new DadosInvalidosException("text or media is required");
        }
        return new Mensagem(null, conversaId, remetenteId, temTexto ? texto.trim() : null, List.of(), agora);
    }

    public Mensagem comMidias(List<String> urls) {
        return new Mensagem(id, conversaId, remetenteId, texto, List.copyOf(urls), criadaEm);
    }
}
