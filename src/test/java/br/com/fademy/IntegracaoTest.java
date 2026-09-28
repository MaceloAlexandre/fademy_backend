package br.com.fademy;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Base dos testes de integração. Sobe Postgres, Mongo e SeaweedFS (S3) uma vez por execução e
 * aponta o {@code DatabaseManager} e o armazenamento para eles antes da subida do Spring.
 */
@SpringBootTest
public abstract class IntegracaoTest {

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");
    protected static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8");
    protected static final GenericContainer<?> SEAWEEDFS = new GenericContainer<>("chrislusf/seaweedfs")
            .withCopyFileToContainer(MountableFile.forHostPath("infra/seaweedfs-s3.json"), "/etc/seaweedfs/s3.json")
            .withCommand("server", "-dir=/data", "-s3", "-s3.config=/etc/seaweedfs/s3.json")
            .withExposedPorts(8333)
            .waitingFor(Wait.forListeningPort());

    static {
        POSTGRES.start();
        MONGO.start();
        SEAWEEDFS.start();
        System.setProperty("POSTGRES_URL", POSTGRES.getJdbcUrl());
        System.setProperty("POSTGRES_USER", POSTGRES.getUsername());
        System.setProperty("POSTGRES_PASSWORD", POSTGRES.getPassword());
        System.setProperty("MONGO_URI", MONGO.getReplicaSetUrl("fademy"));

        String s3 = "http://" + SEAWEEDFS.getHost() + ":" + SEAWEEDFS.getMappedPort(8333);
        System.setProperty("S3_ENDPOINT", s3);
        System.setProperty("S3_PUBLIC_URL", s3 + "/fademy");
        criarBucket(s3);
    }

    /** URL que a API devolve para a imagem com esta chave (S3_PUBLIC_URL do teste + chave). */
    protected static String url(String chave) {
        return System.getProperty("S3_PUBLIC_URL") + "/" + chave;
    }

    // No docker-compose quem cria o bucket é o serviço "criar-bucket"; aqui, o próprio teste.
    private static void criarBucket(String endpoint) {
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("fademy", "fademy123")))
                .forcePathStyle(true)
                .build()) {
            for (int tentativa = 1; ; tentativa++) {
                try {
                    s3.createBucket(r -> r.bucket("fademy"));
                    return;
                } catch (RuntimeException e) {
                    // O S3 do SeaweedFS demora alguns segundos a mais que a porta para ficar pronto.
                    if (tentativa == 30) {
                        throw e;
                    }
                    dormir();
                }
            }
        }
    }

    private static void dormir() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
