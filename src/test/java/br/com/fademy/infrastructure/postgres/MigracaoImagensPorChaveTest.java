package br.com.fademy.infrastructure.postgres;

import br.com.fademy.IntegracaoTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigracaoImagensPorChaveTest extends IntegracaoTest {

    private static final String SCHEMA = "migracao_v7";

    @Test
    void urlsAntigasViramChaves() {
        DriverManagerDataSource banco = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(banco).schemas(SCHEMA).target("6").load().migrate();
        JdbcClient jdbc = JdbcClient.create(banco);

        jdbc.sql("""
                insert into migracao_v7.instituicao (id, nome, slug, ativa, criada_em)
                values ('00000000-0000-7000-8000-000000000001', 'Teste', 'teste', true, now());
                insert into migracao_v7.usuario (id, instituicao_id, nome, email, senha_hash, avatar_url, papel, criado_em)
                values ('00000000-0000-7000-8000-000000000002', '00000000-0000-7000-8000-000000000001', 'Com foto',
                        'a@x.com', 'h', 'http://192.168.2.118:8333/fademy/avatars/abc.png', 'aluno', now()),
                       ('00000000-0000-7000-8000-000000000003', '00000000-0000-7000-8000-000000000001', 'Foto estranha',
                        'b@x.com', 'h', 'http://avatar', 'aluno', now()),
                       ('00000000-0000-7000-8000-000000000004', '00000000-0000-7000-8000-000000000001', 'Sem foto',
                        'c@x.com', 'h', null, 'aluno', now());
                insert into migracao_v7.post (id, autor_id, instituicao_id, texto, imagens, criado_em)
                values ('00000000-0000-7000-8000-000000000005', '00000000-0000-7000-8000-000000000002',
                        '00000000-0000-7000-8000-000000000001', 'post',
                        array['http://localhost:8333/fademy/posts/p1.jpg', 'lixo', 'http://x/fademy/posts/p2.webp'],
                        now());
                """).update();

        Flyway.configure().dataSource(banco).schemas(SCHEMA).load().migrate();

        assertThat(chave(jdbc, "Com foto")).isEqualTo("avatars/abc.png");
        assertThat(chave(jdbc, "Foto estranha")).isNull();
        assertThat(chave(jdbc, "Sem foto")).isNull();
        List<String> imagens = jdbc.sql("select imagens from migracao_v7.post")
                .query((rs, n) -> Arrays.asList((String[]) rs.getArray("imagens").getArray())).single();
        assertThat(imagens).isEqualTo(List.of("posts/p1.jpg", "posts/p2.webp"));
    }

    private static String chave(JdbcClient jdbc, String nome) {
        return jdbc.sql("select avatar_chave from migracao_v7.usuario where nome = :nome")
                .param("nome", nome).query(String.class).optional().orElse(null);
    }
}
