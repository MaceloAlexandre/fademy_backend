package br.com.fademy.infrastructure.mongo;

import br.com.fademy.IntegracaoTest;
import br.com.fademy.domain.midia.Imagem;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class MongoArmazenamentoGatewayTest extends IntegracaoTest {

    @Autowired
    MongoArmazenamentoGateway armazenamento;

    @Autowired
    MongoDatabase mongo;

    @Test
    void gravaNoGridFsComAChaveComoNomeEOTipoNosMetadados() {
        Imagem imagem = new Imagem(new byte[]{1, 2, 3}, "image/webp");
        String chave = imagem.novaChave("posts");

        armazenamento.enviar(chave, imagem);

        Document arquivo = mongo.getCollection("imagens.files").find(Filters.eq("filename", chave)).first();
        assertThat(arquivo).isNotNull();
        assertThat(arquivo.get("metadata", Document.class).getString("tipo")).isEqualTo("image/webp");
        assertThat(armazenamento.existe(chave)).isTrue();

        Imagem lida = armazenamento.ler(chave).orElseThrow();
        assertThat(lida.conteudo()).containsExactly(1, 2, 3);
        assertThat(lida.tipo()).isEqualTo("image/webp");
    }

    @Test
    void chaveInexistente() {
        assertThat(armazenamento.existe("posts/nao-existe.png")).isFalse();
        assertThat(armazenamento.ler("posts/nao-existe.png")).isEmpty();
    }
}
