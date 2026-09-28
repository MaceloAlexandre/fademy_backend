package br.com.fademy.domain.instituicao;

import java.time.Instant;

public record Instituicao(String id, String nome, String slug, String logoUrl, boolean ativa, Instant criadaEm) {
}
