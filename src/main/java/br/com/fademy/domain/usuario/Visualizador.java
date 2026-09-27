package br.com.fademy.domain.usuario;

/**
 * Quem faz a requisição, como vem no token (SPEC RN07, RN08).
 */
public record Visualizador(String id, Papel papel, String instituicaoId) {
}
