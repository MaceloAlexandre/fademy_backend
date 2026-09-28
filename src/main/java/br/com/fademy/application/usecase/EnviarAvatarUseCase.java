package br.com.fademy.application.usecase;

/**
 * Upload da foto de perfil, antes de a conta existir (SPEC RN14).
 */
public interface EnviarAvatarUseCase {

    /** Devolve a chave da foto no armazenamento. */
    String executar(byte[] conteudo, String tipo);
}
