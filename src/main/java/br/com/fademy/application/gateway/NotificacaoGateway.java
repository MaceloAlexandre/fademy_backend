package br.com.fademy.application.gateway;

import br.com.fademy.domain.notificacao.Notificacao;

import java.util.List;
import java.util.Optional;

/**
 * Notificações no MongoDB.
 */
public interface NotificacaoGateway {

    /**
     * Grava a notificação. Notificação é efeito colateral e nunca derruba a ação principal
     * (SPEC RN37): em caso de falha, a implementação registra no log e não lança exceção.
     */
    void criar(Notificacao notificacao);

    /** Apaga o pedido de seguir do ator para o destinatário, se houver. Mesma regra de falha do criar. */
    void removerPedidoDeSeguir(String atorId, String destinatarioId);

    /** Apaga os pedidos de seguir entre A e B nos dois sentidos. Mesma regra de falha do criar. */
    void removerPedidosDeSeguirEntre(String usuarioA, String usuarioB);

    /**
     * Notificações do destinatário, da mais nova para a mais antiga, só as anteriores ao cursor
     * (quando há cursor).
     *
     * @throws br.com.fademy.domain.excecao.DadosInvalidosException se o cursor não é um id válido
     */
    List<Notificacao> listar(String destinatarioId, String cursor, int quantidade);

    long contarNaoLidas(String destinatarioId);

    Optional<Notificacao> buscarPorId(String id);

    void marcarComoLida(String id);

    void remover(String id);
}
