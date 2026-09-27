package br.com.fademy.domain.excecao;

/**
 * Quem faz a requisição não tem permissão (403).
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
