package br.com.fademy.infrastructure.mongo;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.domain.midia.Imagem;
import br.com.fademy.infrastructure.database.MongoAdapter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MongoArmazenamentoGateway implements ArmazenamentoGateway {

    private final MongoAdapter mongo;

    public MongoArmazenamentoGateway(MongoAdapter mongo) {
        this.mongo = mongo;
    }

    @Override
    public void enviar(String chave, Imagem imagem) {
        mongo.salvarArquivo(chave, imagem.conteudo(), imagem.tipo());
    }

    @Override
    public boolean existe(String chave) {
        return mongo.existeArquivo(chave);
    }

    @Override
    public Optional<Imagem> ler(String chave) {
        return mongo.lerArquivo(chave).map(arquivo -> new Imagem(arquivo.conteudo(), arquivo.tipo()));
    }
}
