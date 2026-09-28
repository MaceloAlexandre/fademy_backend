package br.com.fademy.infrastructure.armazenamento;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AvatarDeIniciais {

    public static final int TAMANHO = 96;

    private static final List<Color> PALETA = List.of(
            new Color(0x1B4F81), new Color(0x7B5EA7), new Color(0xE07A5F),
            new Color(0x3D405B), new Color(0x81B29A), new Color(0xC08B2C));

    private AvatarDeIniciais() {
    }

    public static String iniciais(String nome) {
        List<String> palavras = Arrays.stream(nome.trim().split("\\s+"))
                .filter(p -> !p.isEmpty() && !p.endsWith("."))
                .toList();
        if (palavras.isEmpty()) {
            return "?";
        }
        String primeira = palavras.getFirst().substring(0, 1);
        String ultima = palavras.size() > 1 ? palavras.getLast().substring(0, 1) : "";
        return (primeira + ultima).toUpperCase(Locale.ROOT);
    }

    public static Color corPara(String nome) {
        return PALETA.get(Math.floorMod(nome.hashCode(), PALETA.size()));
    }

    public static byte[] png(String nome, Color cor) {
        BufferedImage imagem = new BufferedImage(TAMANHO, TAMANHO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(cor);
        g.fillRect(0, 0, TAMANHO, TAMANHO);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, TAMANHO * 2 / 5));
        String texto = iniciais(nome);
        FontMetrics medidas = g.getFontMetrics();
        g.drawString(texto, (TAMANHO - medidas.stringWidth(texto)) / 2,
                (TAMANHO - medidas.getHeight()) / 2 + medidas.getAscent());
        g.dispose();
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
