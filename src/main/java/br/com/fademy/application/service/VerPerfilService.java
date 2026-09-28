package br.com.fademy.application.service;

import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.VerPerfilUseCase;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

public class VerPerfilService implements VerPerfilUseCase {

    private final UsuarioGateway usuarios;
    private final SeguirGateway seguires;
    private final BloqueioGateway bloqueios;

    public VerPerfilService(UsuarioGateway usuarios, SeguirGateway seguires, BloqueioGateway bloqueios) {
        this.usuarios = usuarios;
        this.seguires = seguires;
        this.bloqueios = bloqueios;
    }

    @Override
    public Perfil executar(Visualizador visualizador, String usuarioId) {
        Usuario eu = usuarios.doVisualizador(visualizador);
        Usuario alvo = usuarios.buscarPorId(usuarioId)
                .filter(eu::mesmaInstituicao)
                .filter(u -> !bloqueios.bloqueou(u.id(), eu.id()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("User not found"));

        boolean bloqueadoPorMim = bloqueios.bloqueou(eu.id(), alvo.id());
        StatusSeguir meuFollow = seguires.buscar(eu.id(), alvo.id()).map(Seguir::status).orElse(null);

        if (!alvo.mostraConteudoPara(eu.id(), meuFollow == StatusSeguir.ACCEPTED)) {
            return new Perfil(alvo, meuFollow, bloqueadoPorMim, null, null);
        }
        return new Perfil(alvo, meuFollow, bloqueadoPorMim,
                seguires.contarSeguidores(alvo.id()), seguires.contarSeguindo(alvo.id()));
    }
}
