package br.com.fademy.application.usecase;

/**
 * Login por e-mail e senha; devolve o token (SPEC RN07).
 */
public interface AutenticarUseCase {

    String executar(String email, String senha);
}
