package br.com.fademy.infrastructure.config;

import br.com.fademy.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class SemPerfilDemoTest extends IntegracaoTest {

    @Autowired
    ApplicationContext contexto;

    @Test
    void semOPerfilDemoNadaDeDemonstracaoSobe() {
        assertThat(contexto.getBeansOfType(DadosDeDemonstracao.class)).isEmpty();
    }
}
