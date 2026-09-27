package br.com.fademy.application.usecase;

/**
 * Arquivo como chegou na requisição, antes de qualquer validação.
 */
public record ArquivoEnviado(byte[] conteudo, String tipo) {
}
