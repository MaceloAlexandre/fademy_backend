package br.com.fademy.application.service;

import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.AutenticarUseCase;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.Usuario;

public class AutenticarService implements AutenticarUseCase {

    private final UsuarioGateway usuarios;
    private final SenhaGateway senhas;
    private final TokenGateway tokens;

    public AutenticarService(UsuarioGateway usuarios, SenhaGateway senhas, TokenGateway tokens) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
    }

    @Override
    public String executar(String email, String senha) {
        if (email == null || email.isEmpty() || senha == null || senha.isEmpty()) {
            throw new DadosInvalidosException("email and password are required");
        }

        // Mesma resposta para e-mail inexistente, senha errada e usuário inativo (SPEC RN07).
        Usuario usuario = usuarios.buscarPorEmail(email)
                .filter(Usuario::ativo)
                .filter(u -> senhas.confere(senha, u.senhaHash()))
                .orElseThrow(() -> new NaoAutenticadoException("Invalid credentials"));

        return tokens.gerar(usuario);
    }
}
