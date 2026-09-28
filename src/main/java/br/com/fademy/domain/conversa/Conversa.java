package br.com.fademy.domain.conversa;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.usuario.Usuario;

import java.time.Instant;
import java.util.List;

public record Conversa(String id, List<String> membros, String criadaPorId, Instant criadaEm, Instant atualizadaEm) {

    public static Conversa entre(Usuario criador, Usuario outro, Instant agora) {
        return new Conversa(null, List.of(criador.id(), outro.id()), criador.id(), agora, agora);
    }

    public boolean temMembro(String usuarioId) {
        return membros.contains(usuarioId);
    }

    public void garantirMembro(String usuarioId) {
        if (!temMembro(usuarioId)) {
            throw new AcessoNegadoException("Forbidden");
        }
    }

    public String outroMembro(String usuarioId) {
        return membros.stream().filter(m -> !m.equals(usuarioId)).findFirst().orElseThrow();
    }
}
