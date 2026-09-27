package br.com.fademy.infrastructure.armazenamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Cliente S3 apontando para o SeaweedFS do docker-compose (ou outro serviço compatível).
 */
@Configuration
public class ArmazenamentoConfig {

    @Bean
    public S3Client s3Client(@Value("${fademy.s3.endpoint}") String endpoint,
                             @Value("${fademy.s3.regiao}") String regiao,
                             @Value("${fademy.s3.chave-de-acesso}") String chaveDeAcesso,
                             @Value("${fademy.s3.chave-secreta}") String chaveSecreta) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(regiao))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(chaveDeAcesso, chaveSecreta)))
                .forcePathStyle(true)
                // Serviços compatíveis com S3 nem sempre aceitam os checksums novos da AWS.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
