package br.com.fademy.application.gateway;

import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioGateway {

    Optional<Usuario> buscarPorId(String id);

    default Usuario doVisualizador(Visualizador visualizador) {
        return buscarPorId(visualizador.id())
                .orElseThrow(() -> new NaoAutenticadoException("Invalid or expired token"));
    }

    List<Usuario> buscarPorIds(Collection<String> ids);

    Optional<Usuario> buscarPorEmail(String email);

    Usuario criar(Usuario usuario);

    List<Usuario> buscarPorNome(Usuario visualizador, String trecho, int deslocamento, int limite);

    long contarPorNome(Usuario visualizador, String trecho);

    List<Usuario> candidatosASugestao(Usuario visualizador);

    List<Usuario> seguidoresMutuos(Usuario usuario);

    long contarDaInstituicao(String instituicaoId);

    void atualizar(Usuario usuario);
}
