package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.ListarCategoriasUseCase;
import br.com.fademy.domain.instituicao.Categoria;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoriaController {

    private final ListarCategoriasUseCase listarCategorias;

    public CategoriaController(ListarCategoriasUseCase listarCategorias) {
        this.listarCategorias = listarCategorias;
    }

    @GetMapping("/api/categories")
    public CategoriasResponse listar() {
        return new CategoriasResponse(listarCategorias.executar().stream().map(CategoriaResponse::de).toList());
    }

    public record CategoriasResponse(List<CategoriaResponse> categories) {
    }

    public record CategoriaResponse(@JsonProperty("_id") String id, String name, String slug, String type) {

        static CategoriaResponse de(Categoria c) {
            return new CategoriaResponse(c.id(), c.nome(), c.slug(), c.tipo().texto());
        }
    }
}
