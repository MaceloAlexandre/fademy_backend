package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.InstituicaoGateway;
import br.com.fademy.domain.instituicao.Instituicao;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class PostgresInstituicaoGateway implements InstituicaoGateway {

    private final JdbcClient jdbc;

    public PostgresInstituicaoGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Instituicao garantir(Instituicao instituicao) {
        String id = jdbc.sql("""
                        insert into instituicao (id, nome, slug, logo_url, ativa, criada_em)
                        values (:id, :nome, :slug, :logoUrl, :ativa, :criadaEm)
                        on conflict (slug) do update set nome = excluded.nome, ativa = excluded.ativa
                        returning id
                        """)
                .param("id", Ids.novo())
                .param("nome", instituicao.nome())
                .param("slug", instituicao.slug())
                .param("logoUrl", instituicao.logoUrl())
                .param("ativa", instituicao.ativa())
                .param("criadaEm", Timestamp.from(instituicao.criadaEm()))
                .query(String.class)
                .single();
        return new Instituicao(id, instituicao.nome(), instituicao.slug(), instituicao.logoUrl(),
                instituicao.ativa(), instituicao.criadaEm());
    }
}
