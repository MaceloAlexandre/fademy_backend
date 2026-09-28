package br.com.fademy.infrastructure.armazenamento;

import br.com.fademy.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LimiteDeUploadTest extends IntegracaoTest {

    @LocalServerPort
    int porta;

    @Test
    void arquivoAcimaDoLimiteDa400() {
        HttpHeaders tipo = new HttpHeaders();
        tipo.setContentType(MediaType.IMAGE_JPEG);
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("file", new HttpEntity<>(new ByteArrayResource(new byte[5 * 1024 * 1024 + 1]) {
            @Override
            public String getFilename() {
                return "grande.jpg";
            }
        }, tipo));

        ResponseEntity<String> resposta = RestClient.create("http://localhost:" + porta)
                .post().uri("/api/media/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(corpo)
                .retrieve()
                .onStatus(status -> true, (requisicao, r) -> { })
                .toEntity(String.class);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody()).isEqualTo("{\"error\":\"File upload error\"}");
    }
}
