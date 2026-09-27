package br.com.fademy.application.gateway;

import br.com.fademy.domain.conversa.Conversa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Conversas no MongoDB.
 */
public interface ConversaGateway {

    /**
     * Devolve a conversa entre os dois membros, criando-a se não existe. Existe no máximo uma
     * conversa por par, mesmo com pedidos simultâneos (SPEC RN38).
     */
    Resultado buscarOuCriar(Conversa conversa);

    Optional<Conversa> buscarPorId(String id);

    /** Marca atividade na conversa: ela sobe na lista dos membros (SPEC RN39). */
    void registrarAtividade(String conversaId, Instant quando);

    /** Conversas do usuário, da atividade mais recente para a mais antiga. */
    List<Conversa> doUsuario(String usuarioId);

    record Resultado(Conversa conversa, boolean criada) {
    }
}
