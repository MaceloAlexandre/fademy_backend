package br.com.fademy.application.gateway;

import br.com.fademy.domain.instituicao.Categoria;

import java.util.Collection;
import java.util.List;

public interface CategoriaGateway {

    void garantir(List<Categoria> categorias);

    List<Categoria> listar();

    List<Categoria> buscarPorIds(Collection<String> ids);

    boolean existem(Collection<String> ids);
}
