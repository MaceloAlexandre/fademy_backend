package br.com.fademy.infrastructure.config;

import br.com.fademy.IntegracaoTest;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.AutenticarUseCase;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("demo")
class DadosDeDemonstracaoTest extends IntegracaoTest {

    @Autowired
    DadosDeDemonstracao dadosDeDemonstracao;

    @Autowired
    UsuarioGateway usuarios;

    @Autowired
    AutenticarUseCase autenticar;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MongoDatabase mongo;

    @Test
    void criaAsPessoasPeloConviteEElasEntramComASenhaDoRoteiro() {
        for (String email : new String[]{"erlon@demo.fademy", "rene@demo.fademy", "fabricio@demo.fademy",
                "erika@demo.fademy", "wacelys@demo.fademy", "artur@demo.fademy"}) {
            assertThat(usuarios.buscarPorEmail(email)).as(email).isPresent();
            assertThat(autenticar.executar(email, DadosDeDemonstracao.SENHA)).isNotBlank();
        }
        assertThat(usuarios.buscarPorEmail("erlon@demo.fademy").orElseThrow().papel().texto()).isEqualTo("professor");
        assertThat(usuarios.buscarPorEmail("artur@demo.fademy").orElseThrow().privado()).isTrue();
        assertThat(contar("select count(*) from convite c join usuario u on u.id = c.convidado_por_id"
                + " where u.papel = 'admin' and c.status = 'expired' and c.usos = 1")).isGreaterThanOrEqualTo(6);
    }

    @Test
    void criaFollowsPostsComentariosEConversa() {
        assertThat(contar(demo("select count(*) from seguir s join usuario u on u.id = s.seguidor_id where %s"
                + " and s.status = 'pending'"))).isEqualTo(1);
        assertThat(contar(demo("select count(*) from seguir s join usuario u on u.id = s.seguidor_id where %s"
                + " and s.status = 'accepted'"))).isEqualTo(11);
        assertThat(contar(demo("select count(*) from post p join usuario u on u.id = p.autor_id where %s"))).isEqualTo(5);
        assertThat(contar(demo("select count(*) from comentario c join usuario u on u.id = c.autor_id where %s")))
                .isEqualTo(2);
        String wacelys = usuarios.buscarPorEmail("wacelys@demo.fademy").orElseThrow().id();
        assertThat(mongo.getCollection("conversas").countDocuments(new org.bson.Document("membros", wacelys))).isEqualTo(1);
        assertThat(mongo.getCollection("mensagens").countDocuments()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void rodarDeNovoNaoDuplica() throws Exception {
        long usuariosAntes = contar("select count(*) from usuario");
        long postsAntes = contar("select count(*) from post");

        dadosDeDemonstracao.run(null);

        assertThat(contar("select count(*) from usuario")).isEqualTo(usuariosAntes);
        assertThat(contar("select count(*) from post")).isEqualTo(postsAntes);
    }

    private static String demo(String sql) {
        return sql.formatted("u.email like '%@demo.fademy'");
    }

    private long contar(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }
}
