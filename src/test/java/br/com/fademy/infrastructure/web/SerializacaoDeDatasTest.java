package br.com.fademy.infrastructure.web;

import br.com.fademy.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SerializacaoDeDatasTest extends IntegracaoTest {

    @Autowired
    JsonMapper jsonMapper;

    record ComData(Instant createdAt) {
    }

    @Test
    void instantViraIsoEmUtcComZ() {
        String json = jsonMapper.writeValueAsString(new ComData(Instant.parse("2026-09-27T18:00:00Z")));

        assertThat(json).isEqualTo("{\"createdAt\":\"2026-09-27T18:00:00Z\"}");
    }
}
