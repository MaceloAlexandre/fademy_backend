package br.com.fademy.application.usecase;

public interface VerificarSaudeUseCase {

    Saude executar();

    record Saude(boolean postgresNoAr, boolean mongoNoAr) {

        public boolean tudoNoAr() {
            return postgresNoAr && mongoNoAr;
        }
    }
}
