package br.com.fademy.application.service;

/**
 * Tamanho de página pedido pelo cliente: valor ausente, inválido ou menor que 1 vira o padrão,
 * e nada passa do máximo (SPEC 5.4, RN19, RN20).
 */
final class Limites {

    private Limites() {
    }

    static int ler(String pedido, int padrao, int maximo) {
        return Math.min(inteiroPositivo(pedido, padrao), maximo);
    }

    static int inteiroPositivo(String pedido, int padrao) {
        try {
            int valor = Integer.parseInt(pedido.trim());
            return valor >= 1 ? valor : padrao;
        } catch (NumberFormatException | NullPointerException e) {
            return padrao;
        }
    }
}
