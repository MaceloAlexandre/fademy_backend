package br.com.fademy.application.service;

import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.SugerirUsuariosUseCase;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.Comparator;
import java.util.List;

public class SugerirUsuariosService implements SugerirUsuariosUseCase {

    private final UsuarioGateway usuarios;

    public SugerirUsuariosService(UsuarioGateway usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public List<Usuario> executar(Visualizador visualizador, String limite) {
        Usuario eu = usuarios.doVisualizador(visualizador);
        return usuarios.candidatosASugestao(eu).stream()
                .sorted(Comparator.comparing(usuario -> !usuario.ehDaEquipe()))
                .limit(Limites.ler(limite, 15, 20))
                .toList();
    }
}
