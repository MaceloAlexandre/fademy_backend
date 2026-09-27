package br.com.fademy.application.gateway;

public interface SenhaGateway {

    String gerarHash(String senha);

    boolean confere(String senha, String hash);
}
