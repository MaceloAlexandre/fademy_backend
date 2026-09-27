package br.com.fademy.application.gateway;

import br.com.fademy.domain.instituicao.Categoria;

import java.util.Collection;
import java.util.List;

public interface CategoriaGateway {

    /** Cria cada categoria ou, se o slug já existe, atualiza nome e tipo. */
    void garantir(List<Categoria> categorias);

    List<Categoria> listar();

    /** Todos os ids existem. */
    boolean existem(Collection<String> ids);
}
