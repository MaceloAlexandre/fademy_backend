package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Usuario;

public interface RegistrarViaConviteUseCase {

    Usuario executar(Cadastro cadastro);

    record Cadastro(String token, String nome, String email, String senha, String avatarUrl, String avatarChave) {
    }
}
