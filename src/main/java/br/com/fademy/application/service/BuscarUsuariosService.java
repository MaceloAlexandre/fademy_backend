package br.com.fademy.application.service;

import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.BuscarUsuariosUseCase;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;

public class BuscarUsuariosService implements BuscarUsuariosUseCase {

    private static final int TAMANHO_MAXIMO_DO_TRECHO = 100;

    private final UsuarioGateway usuarios;

    public BuscarUsuariosService(UsuarioGateway usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Resultado executar(Visualizador visualizador, String trecho, String pagina, String limite) {
        int tamanho = Limites.ler(limite, 20, 50);
        if (trecho == null || trecho.isBlank()) {
            return new Resultado(List.of(), 0, 1, tamanho);
        }
        String busca = trecho.length() > TAMANHO_MAXIMO_DO_TRECHO
                ? trecho.substring(0, TAMANHO_MAXIMO_DO_TRECHO)
                : trecho;
        int numeroDaPagina = Limites.inteiroPositivo(pagina, 1);

        Usuario eu = usuarios.doVisualizador(visualizador);
        return new Resultado(
                usuarios.buscarPorNome(eu, busca, (numeroDaPagina - 1) * tamanho, tamanho),
                usuarios.contarPorNome(eu, busca),
                numeroDaPagina,
                tamanho);
    }
}
