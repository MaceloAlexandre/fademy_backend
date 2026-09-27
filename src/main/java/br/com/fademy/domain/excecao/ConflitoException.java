package br.com.fademy.domain.excecao;

/**
 * A operação conflita com o estado atual, como um registro duplicado (409).
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
