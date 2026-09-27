package br.com.fademy.application.gateway;

public interface BloqueioGateway {

    /** Existe bloqueio de A para B ou de B para A. */
    boolean existeEntre(String usuarioA, String usuarioB);
}
