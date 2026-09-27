package br.com.fademy.domain.excecao;

/**
 * O recurso não existe ou não é visível para quem pede (404).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
