package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.CategoriaGateway;
import br.com.fademy.domain.instituicao.Categoria;
import br.com.fademy.domain.instituicao.TipoCategoria;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
    public boolean existem(Collection<String> ids) {
        List<UUID> validos = ids.stream().flatMap(id -> Ids.ler(id).stream()).distinct().toList();
        if (validos.size() < ids.stream().distinct().count()) {
            return false;
        }
        return validos.isEmpty() || jdbc.sql("select count(*) from categoria where id in (:ids)")
                .param("ids", validos)
                .query(Long.class)
                .single() == validos.size();
    }

    @Override
    public List<Categoria> listar() {
        return jdbc.sql("select * from categoria order by nome")
                .query((rs, n) -> new Categoria(rs.getString("id"), rs.getString("nome"), rs.getString("slug"),
                        TipoCategoria.deTexto(rs.getString("tipo"))))
                .list();
    }
}
