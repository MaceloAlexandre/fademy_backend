package br.com.fademy.application.gateway;

import br.com.fademy.domain.social.Seguir;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeguirGateway {

    Optional<Seguir> buscar(String seguidorId, String seguidoId);

    Optional<Seguir> buscarPorId(String id);

    List<Seguir> buscarPorIds(Collection<String> ids);

    void atualizarStatus(Seguir seguir);

    void removerPorId(String id);

    boolean seguemUmAoOutro(String usuarioA, String usuarioB);

    long contarSeguidores(String usuarioId);

    long contarSeguindo(String usuarioId);

    void removerEntre(String usuarioA, String usuarioB);

    Seguir criar(Seguir seguir);

    void remover(String seguidorId, String seguidoId);
}
