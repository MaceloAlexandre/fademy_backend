package br.com.fademy.infrastructure.postgres;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Optional;
import java.util.UUID;

/**
 * Ids das entidades do Postgres: UUIDv7, ordenado pelo tempo, para a paginação por cursor
 * funcionar como no ObjectId do Node (docs/ARQUITETURA.md, seção 8).
 */
final class Ids {

    private Ids() {
    }

    static UUID novo() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /** Id vindo de fora (path, token); formato inválido vale como inexistente (SPEC 5.1). */
    static Optional<UUID> ler(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
