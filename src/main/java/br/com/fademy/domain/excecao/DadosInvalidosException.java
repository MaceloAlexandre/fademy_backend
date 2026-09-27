package br.com.fademy.domain.excecao;

/**
 * Dados da requisição inválidos (400).
 */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
