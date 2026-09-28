package br.com.fademy.application.gateway;

import br.com.fademy.domain.conversa.Conversa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConversaGateway {

    Resultado buscarOuCriar(Conversa conversa);

    Optional<Conversa> buscarPorId(String id);

    void registrarAtividade(String conversaId, Instant quando);

    List<Conversa> doUsuario(String usuarioId);

    record Resultado(Conversa conversa, boolean criada) {
    }
}
