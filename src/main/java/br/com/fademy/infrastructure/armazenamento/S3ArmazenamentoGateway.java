package br.com.fademy.infrastructure.armazenamento;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.domain.midia.Imagem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Component
public class S3ArmazenamentoGateway implements ArmazenamentoGateway {

    private final S3Client s3;
    private final String bucket;

    public S3ArmazenamentoGateway(S3Client s3, @Value("${fademy.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void enviar(String chave, Imagem imagem) {
        s3.putObject(r -> r.bucket(bucket).key(chave).contentType(imagem.tipo()),
                RequestBody.fromBytes(imagem.conteudo()));
    }

    @Override
    public boolean existe(String chave) {
        try {
            s3.headObject(r -> r.bucket(bucket).key(chave));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
