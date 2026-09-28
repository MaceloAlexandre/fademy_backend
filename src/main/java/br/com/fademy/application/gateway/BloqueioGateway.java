package br.com.fademy.application.gateway;

public interface BloqueioGateway {

    boolean existeEntre(String usuarioA, String usuarioB);

    boolean bloqueou(String bloqueadorId, String bloqueadoId);

    void bloquear(String bloqueadorId, String bloqueadoId);

    void desbloquear(String bloqueadorId, String bloqueadoId);
}
