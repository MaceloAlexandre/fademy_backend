package br.com.fademy.application.service;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.application.usecase.ListarCategoriasUseCase;
import br.com.fademy.domain.instituicao.Categoria;

import java.util.List;

public class ListarCategoriasService implements ListarCategoriasUseCase {

    private final CategoriaGateway categorias;

    public ListarCategoriasService(CategoriaGateway categorias) {
        this.categorias = categorias;
    }

    @Override
    public List<Categoria> executar() {
        return categorias.listar();
    }
}
