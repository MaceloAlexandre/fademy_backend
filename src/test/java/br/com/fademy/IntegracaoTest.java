package br.com.fademy;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração. Sobe Postgres e Mongo uma vez por execução e aponta o
 * {@code DatabaseManager} para eles antes da subida do Spring. As imagens ficam no GridFS do
 * mesmo Mongo (#62).
 */
@SpringBootTest
public abstract class IntegracaoTest {

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");
    protected static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8");

    static {
        POSTGRES.start();
        MONGO.start();
        System.setProperty("POSTGRES_URL", POSTGRES.getJdbcUrl());
        System.setProperty("POSTGRES_USER", POSTGRES.getUsername());
        System.setProperty("POSTGRES_PASSWORD", POSTGRES.getPassword());
        System.setProperty("MONGO_URI", MONGO.getReplicaSetUrl("fademy"));
    }

    /** URL que a API devolve para a imagem com esta chave (IMAGENS_URL_PUBLICA padrão + chave). */
    protected static String url(String chave) {
        return "http://localhost:8080/api/media/" + chave;
    }
}
