package br.com.fademy.application.gateway;

import br.com.fademy.domain.notificacao.Notificacao;

import java.util.List;
import java.util.Optional;

public interface NotificacaoGateway {

    void criar(Notificacao notificacao);

    void removerPedidoDeSeguir(String atorId, String destinatarioId);

    void removerPedidosDeSeguirEntre(String usuarioA, String usuarioB);

    List<Notificacao> listar(String destinatarioId, String cursor, int quantidade);

    long contarNaoLidas(String destinatarioId);

    Optional<Notificacao> buscarPorId(String id);

    void marcarComoLida(String id);

    void remover(String id);
}
