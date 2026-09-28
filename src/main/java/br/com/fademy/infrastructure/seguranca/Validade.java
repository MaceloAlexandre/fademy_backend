package br.com.fademy.infrastructure.seguranca;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Validade {

    private static final Pattern FORMATO = Pattern.compile("(\\d+)([dhms]?)");

    private Validade() {
    }

    static Duration ler(String texto) {
        Matcher m = FORMATO.matcher(texto.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("JWT_EXPIRES_IN inválido: " + texto);
        }
        long valor = Long.parseLong(m.group(1));
        return switch (m.group(2)) {
            case "d" -> Duration.ofDays(valor);
            case "h" -> Duration.ofHours(valor);
            case "m" -> Duration.ofMinutes(valor);
            default -> Duration.ofSeconds(valor);
        };
    }
}
