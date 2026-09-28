package br.com.fademy.application.usecase;

import br.com.fademy.domain.midia.Imagem;

/**
 * Devolve uma imagem guardada, pela chave (SPEC RN49).
 */
public interface VerImagemUseCase {

    Imagem executar(String chave);
}
