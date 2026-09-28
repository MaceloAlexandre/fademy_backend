package br.com.fademy.domain.usuario;

import java.time.Instant;

public record DadosUsuario(
        String id,
        String instituicaoId,
        String nome,
        String email,
        String senhaHash,
        String avatarChave,
        boolean privado,
        boolean ativo,
        Instant criadoEm) {
}
