package br.com.fademy.infrastructure.armazenamento;

import br.com.fademy.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O limite de tamanho é aplicado pelo servidor HTTP ao ler o multipart; o MockMvc não passa
 * por essa leitura, então este teste sobe o servidor de verdade.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LimiteDeUploadTest extends IntegracaoTest {

    @LocalServerPort
    int porta;

    @Test
    void arquivoAcimaDoLimiteDa400() {
        MultipartBodyBuilder corpo = new MultipartBodyBuilder();
        corpo.part("file", new ByteArrayResource(new byte[5 * 1024 * 1024 + 1]) {
            @Override
            public String getFilename() {
                return "grande.jpg";
            }
        }).contentType(MediaType.IMAGE_JPEG);

        ResponseEntity<String> resposta = RestClient.create("http://localhost:" + porta)
                .post().uri("/api/media/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(corpo.build())
                .retrieve()
                .onStatus(status -> true, (requisicao, r) -> { })
                .toEntity(String.class);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody()).isEqualTo("{\"error\":\"File upload error\"}");
    }
}
