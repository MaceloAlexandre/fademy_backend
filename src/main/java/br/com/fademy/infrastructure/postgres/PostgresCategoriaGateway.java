package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.instituicao.TipoCategoria;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostgresCategoriaGateway implements CategoriaGateway {

    private final JdbcClient jdbc;

    public PostgresCategoriaGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void garantir(List<Categoria> categorias) {
        for (Categoria categoria : categorias) {
            jdbc.sql("""
                            insert into categoria (id, nome, slug, tipo) values (:id, :nome, :slug, :tipo)
                            on conflict (slug) do update set nome = excluded.nome, tipo = excluded.tipo
                            """)
                    .param("id", Ids.novo())
                    .param("nome", categoria.nome())
                    .param("slug", categoria.slug())
                    .param("tipo", categoria.tipo().texto())
                    .update();
        }
    }

    @Override
    public List<Categoria> listar() {
        return jdbc.sql("select * from categoria order by nome")
                .query((rs, n) -> new Categoria(rs.getString("id"), rs.getString("nome"), rs.getString("slug"),
                        TipoCategoria.deTexto(rs.getString("tipo"))))
                .list();
    }
}
