package br.com.fademy.application.usecase;

import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

/**
 * Edição do próprio perfil: privacidade e foto (SPEC RN18).
 */
public interface EditarPerfilUseCase {

    Usuario executar(Visualizador visualizador, EdicaoDePerfil edicao);

    /**
     * Campos pedidos, com os valores como vieram (o tipo é validado pelo caso de uso).
     *
     * @param alteraPrivacidade o pedido trouxe a privacidade
     * @param alteraAvatar      o pedido trouxe a URL ou a chave da foto
     */
    record EdicaoDePerfil(boolean alteraPrivacidade, Object privado,
                          boolean alteraAvatar, Object avatarUrl, Object avatarChave) {
    }
}
