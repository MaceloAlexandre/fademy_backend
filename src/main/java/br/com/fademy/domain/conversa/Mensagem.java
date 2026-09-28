package br.com.fademy.domain.conversa;

import br.com.fademy.domain.excecao.DadosInvalidosException;

import java.time.Instant;
import java.util.List;

public record Mensagem(String id, String conversaId, String remetenteId, String texto, List<String> midias,
                       Instant criadaEm) {

    public static Mensagem escrever(String conversaId, String remetenteId, String texto, int quantidadeDeImagens,
                                    Instant agora) {
        boolean temTexto = texto != null && !texto.isBlank();
        if (!temTexto && quantidadeDeImagens == 0) {
            throw new DadosInvalidosException("text or media is required");
        }
        return new Mensagem(null, conversaId, remetenteId, temTexto ? texto.trim() : null, List.of(), agora);
    }

    public Mensagem comMidias(List<String> chaves) {
        return new Mensagem(id, conversaId, remetenteId, texto, List.copyOf(chaves), criadaEm);
    }
}
