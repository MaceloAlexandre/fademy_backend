package br.com.fademy.domain.usuario;

import java.time.Instant;

/**
 * Dados de um usuário, comuns a todos os papéis. A foto é a chave no armazenamento; a URL é
 * montada só na hora de responder (docs/ARQUITETURA.md, seção 7). Usado para criar qualquer subclasse de
 * {@link Usuario} sem repetir o mesmo construtor longo em cada uma.
 */
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
