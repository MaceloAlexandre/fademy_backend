package br.com.fademy.infrastructure.database;

/**
 * Endereços e credenciais dos bancos. Sem configuração, usa os bancos do docker-compose
 * em localhost, como o exemplo do professor.
 */
public record ConfiguracaoBancos(String postgresUrl, String postgresUsuario, String postgresSenha, String mongoUri) {

    public static ConfiguracaoBancos doAmbiente() {
        return new ConfiguracaoBancos(
                ler("POSTGRES_URL", "jdbc:postgresql://localhost:5432/fademy"),
                ler("POSTGRES_USER", "fademy"),
                ler("POSTGRES_PASSWORD", "fademy"),
                ler("MONGO_URI", "mongodb://localhost:27017/fademy"));
    }

    // Propriedade de sistema primeiro (usada pelos testes), depois variável de ambiente.
    private static String ler(String chave, String padrao) {
        String valor = System.getProperty(chave);
        if (valor == null) {
            valor = System.getenv(chave);
        }
        return valor != null ? valor : padrao;
    }
}
