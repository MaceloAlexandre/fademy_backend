package br.com.fademy.application.usecase;

import br.com.fademy.domain.midia.Imagem;

public interface VerImagemUseCase {

    Imagem executar(String chave);
}
