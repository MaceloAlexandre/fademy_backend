package br.com.fademy.application.usecase;

/**
 * Upload da foto de perfil, antes de a conta existir (SPEC RN14).
 */
public interface EnviarAvatarUseCase {

    ImagemEnviada executar(byte[] conteudo, String tipo);

    record ImagemEnviada(String chave, String url) {
    }
}
