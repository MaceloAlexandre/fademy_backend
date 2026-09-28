package br.com.fademy.application.usecase;

public interface EnviarAvatarUseCase {

    String executar(byte[] conteudo, String tipo);
}
