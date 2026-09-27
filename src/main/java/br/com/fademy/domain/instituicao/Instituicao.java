package br.com.fademy.domain.instituicao;

import java.time.Instant;

/**
 * Tenant: todo usuário pertence a exatamente uma instituição (SPEC RN01).
 */
public record Instituicao(String id, String nome, String slug, String logoUrl, boolean ativa, Instant criadaEm) {
}
