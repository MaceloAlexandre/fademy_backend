package br.com.fademy.application.service;

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
