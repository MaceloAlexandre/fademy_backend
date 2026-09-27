package br.com.fademy.domain.excecao;

/**
 * Quem faz a requisição não está autenticado (401).
 */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
