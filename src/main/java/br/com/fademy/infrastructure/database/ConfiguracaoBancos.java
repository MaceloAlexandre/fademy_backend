package br.com.fademy.infrastructure.database;

public record ConfiguracaoBancos(String postgresUrl, String postgresUsuario, String postgresSenha, String mongoUri) {

    public static ConfiguracaoBancos doAmbiente() {
        return new ConfiguracaoBancos(
                ler("POSTGRES_URL", "jdbc:postgresql://localhost:5433/fademy"),
                ler("POSTGRES_USER", "fademy"),
                ler("POSTGRES_PASSWORD", "fademy"),
                ler("MONGO_URI", "mongodb://localhost:27017/fademy"));
    }

    private static String ler(String chave, String padrao) {
        String valor = System.getProperty(chave);
        if (valor == null) {
            valor = System.getenv(chave);
        }
        return valor != null ? valor : padrao;
    }
}
