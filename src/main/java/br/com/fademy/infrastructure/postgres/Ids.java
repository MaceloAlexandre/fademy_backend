package br.com.fademy.infrastructure.postgres;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Optional;
import java.util.UUID;

final class Ids {

    private Ids() {
    }

    static UUID novo() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    static Optional<UUID> ler(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
