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

    /** A segue B e B segue A, os dois com follow aceito. */
    boolean seguemUmAoOutro(String usuarioA, String usuarioB);

    /** Quantos seguem o usuário com follow aceito. */
    long contarSeguidores(String usuarioId);

    /** Quantos o usuário segue com follow aceito. */
    long contarSeguindo(String usuarioId);

    /** Remove os follows entre A e B nos dois sentidos. */
    void removerEntre(String usuarioA, String usuarioB);

    /**
     * Grava um follow novo e o devolve com id.
     *
     * @throws br.com.fademy.domain.excecao.ConflitoException se o par já existe
     */
    Seguir criar(Seguir seguir);

    /** Remove o follow do seguidor para o seguido, em qualquer status; sem follow, não faz nada. */
    void remover(String seguidorId, String seguidoId);
}
