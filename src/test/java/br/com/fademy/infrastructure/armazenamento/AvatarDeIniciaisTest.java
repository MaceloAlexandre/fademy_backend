package br.com.fademy.infrastructure.armazenamento;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AvatarDeIniciaisTest {

    @ParameterizedTest
    @CsvSource({
            "Profa. Helena Duarte, HD",
            "Coord. Marcos Lima,   ML",
            "Ana Souza,            AS",
            "maria da silva,       MS",
            "Admin,                A",
    })
    void iniciaisSemTitulos(String nome, String iniciais) {
        assertThat(AvatarDeIniciais.iniciais(nome)).isEqualTo(iniciais);
    }

    @Test
    void pngComACorDeFundo() throws IOException {
        BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(
                AvatarDeIniciais.png("Ana Souza", new Color(0x1B4F81))));

        assertThat(imagem.getWidth()).isEqualTo(AvatarDeIniciais.TAMANHO);
        assertThat(imagem.getHeight()).isEqualTo(AvatarDeIniciais.TAMANHO);
        assertThat(imagem.getRGB(1, 1) & 0xFFFFFF).isEqualTo(0x1B4F81);
        assertThat(temPixelBranco(imagem)).as("desenhou as iniciais").isTrue();
    }

    @Test
    void mesmaCorParaOMesmoNome() {
        assertThat(AvatarDeIniciais.corPara("Admin")).isEqualTo(AvatarDeIniciais.corPara("Admin"));
    }

    @Test
    void armazenamentoForaDoArDevolveSemFoto() {
        ArmazenamentoGateway fora = mock();
        doThrow(new RuntimeException("sem conexão")).when(fora).enviar(anyString(), any());

        assertThat(new AvatarDeIniciaisGateway(fora).criar("Admin")).isEmpty();
    }

    private static boolean temPixelBranco(BufferedImage imagem) {
        for (int x = 0; x < imagem.getWidth(); x++) {
            for (int y = 0; y < imagem.getHeight(); y++) {
                if ((imagem.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                    return true;
                }
            }
        }
        return false;
    }
}
