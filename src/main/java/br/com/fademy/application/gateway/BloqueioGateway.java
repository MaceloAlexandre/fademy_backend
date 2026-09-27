package br.com.fademy.application.gateway;

public interface BloqueioGateway {

    /** Existe bloqueio de A para B ou de B para A. */
    boolean existeEntre(String usuarioA, String usuarioB);

    /** Registra o bloqueio; se já existe, não faz nada. */
    void bloquear(String bloqueadorId, String bloqueadoId);

    /** Remove o bloqueio; se não existe, não faz nada. */
    void desbloquear(String bloqueadorId, String bloqueadoId);
}
