package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.EditarPerfilUseCase;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public class EditarPerfilService implements EditarPerfilUseCase {

    private final UsuarioGateway usuarios;
    private final ArmazenamentoGateway armazenamento;

    public EditarPerfilService(UsuarioGateway usuarios, ArmazenamentoGateway armazenamento) {
        this.usuarios = usuarios;
        this.armazenamento = armazenamento;
    }

    @Override
    public Usuario executar(Visualizador visualizador, EdicaoDePerfil edicao) {
        if (edicao.alteraPrivacidade() && !(edicao.privado() instanceof Boolean)) {
            throw new DadosInvalidosException("isPrivate must be a boolean");
        }
        if (edicao.alteraAvatar()
                && !(edicao.avatarUrl() instanceof String && edicao.avatarChave() instanceof String)) {
            throw new DadosInvalidosException("avatarUrl and avatarKey must be strings");
        }
        if (!edicao.alteraPrivacidade() && !edicao.alteraAvatar()) {
            throw new DadosInvalidosException("No valid fields to update");
        }

        Usuario eu = usuarios.doVisualizador(visualizador);
        if (edicao.privado() instanceof Boolean privado) {
            eu.definirPrivacidade(privado);
        }
        if (edicao.alteraAvatar()) {
            // A foto precisa ter sido enviada antes, como no cadastro (SPEC D3).
            if (!armazenamento.existe((String) edicao.avatarChave())) {
                throw new DadosInvalidosException(
                        "Profile photo not found in storage — please upload a photo first");
            }
            // Grava a chave validada; a URL enviada pelo cliente não é guardada (#53).
            eu.trocarAvatar((String) edicao.avatarChave());
        }
        usuarios.atualizar(eu);
        return eu;
    }
}
