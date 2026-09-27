package br.com.fademy.application.gateway;

import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;
import java.util.Optional;

public interface UsuarioGateway {

    Optional<Usuario> buscarPorId(String id);

    /** O usuário de quem faz a requisição; se ele não existe mais, o token não vale. */
    default Usuario doVisualizador(Visualizador visualizador) {
        return buscarPorId(visualizador.id())
                .orElseThrow(() -> new NaoAutenticadoException("Invalid or expired token"));
    }

    /** Busca pelo e-mail, sem diferenciar maiúsculas. */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Grava um usuário novo e o devolve com id.
     *
     * @throws br.com.fademy.domain.excecao.ConflitoException se o e-mail já existe (SPEC RN09)
     */
    Usuario criar(Usuario usuario);

    /**
     * Usuários da instituição do visualizador cujo nome contém o trecho (sem diferenciar
     * maiúsculas nem acentos), sem o visualizador e sem quem tem bloqueio com ele, por nome.
     */
    List<Usuario> buscarPorNome(Usuario visualizador, String trecho, int deslocamento, int limite);

    long contarPorNome(Usuario visualizador, String trecho);

    /**
     * Usuários da instituição do visualizador que ele ainda não segue (nem pediu), sem ele e sem
     * bloqueios, do cadastro mais recente para o mais antigo.
     */
    List<Usuario> candidatosASugestao(Usuario visualizador);

    /** Grava privacidade e avatar do usuário. */
    void atualizar(Usuario usuario);
}
