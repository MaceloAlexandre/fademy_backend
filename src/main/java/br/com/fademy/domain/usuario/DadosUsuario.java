package br.com.fademy.domain.usuario;

import java.time.Instant;

/**
 * Dados de um usuário, comuns a todos os papéis. Usado para criar qualquer subclasse de
 * {@link Usuario} sem repetir o mesmo construtor longo em cada uma.
 */
public record DadosUsuario(
        String id,
        String instituicaoId,
        String nome,
        String email,
        String senhaHash,
        String avatarUrl,
        boolean privado,
        boolean ativo,
        Instant criadoEm) {
}
