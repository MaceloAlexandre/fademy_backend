package br.com.fademy.infrastructure.web;

/**
 * Corpo de toda resposta de erro (SPEC seção 5.1).
 */
public record ErroResponse(String error) {
}
