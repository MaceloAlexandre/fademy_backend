package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Usuario;

/**
 * Cadastro de um convidado a partir do link de convite (SPEC RN16).
 */
public interface RegistrarViaConviteUseCase {

    Usuario executar(Cadastro cadastro);

    /**
     * @param avatarUrl   exigido pelo contrato, mas não gravado: a foto vem da chave validada
     * @param avatarChave chave devolvida pelo upload da foto
     */
    record Cadastro(String token, String nome, String email, String senha, String avatarUrl, String avatarChave) {
    }
}
