package br.com.fademy.application.usecase;

import br.com.fademy.domain.instituicao.Categoria;

import java.util.List;

public interface ListarCategoriasUseCase {

    List<Categoria> executar();
}
