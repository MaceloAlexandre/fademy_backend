package br.com.fademy.application.service;

import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.ListarNotificacoesUseCase;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListarNotificacoesService implements ListarNotificacoesUseCase {

    private final NotificacaoGateway notificacoes;
    private final UsuarioGateway usuarios;
    private final SeguirGateway seguires;

    public ListarNotificacoesService(NotificacaoGateway notificacoes, UsuarioGateway usuarios,
                                     SeguirGateway seguires) {
        this.notificacoes = notificacoes;
        this.usuarios = usuarios;
        this.seguires = seguires;
    }

    @Override
    public Resultado executar(Visualizador visualizador, String cursor, String limite) {
        int tamanho = Limites.ler(limite, 20, 50);
        Pagina<Notificacao> pagina = Pagina.de(
                notificacoes.listar(visualizador.id(), cursor, tamanho + 1), tamanho, Notificacao::id);

        // A notificação mora no MongoDB; ator e follow, no Postgres. Junta na leitura (SPEC RN48).
        Map<String, Usuario> atores = usuarios.buscarPorIds(
                        pagina.itens().stream().map(Notificacao::atorId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Usuario::id, Function.identity()));
        Map<String, Seguir> pedidos = seguires.buscarPorIds(
                        pagina.itens().stream()
                                .filter(n -> n.tipo() == TipoNotificacao.FOLLOW_REQUEST)
                                .map(Notificacao::refId)
                                .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Seguir::id, Function.identity()));

        Pagina<NotificacaoDetalhada> detalhada = new Pagina<>(
                pagina.itens().stream().map(n -> new NotificacaoDetalhada(n, atores.get(n.atorId()),
                        pedidos.containsKey(n.refId()) ? pedidos.get(n.refId()).status() : null)).toList(),
                pagina.temMais(), pagina.proximoCursor());
        return new Resultado(detalhada, notificacoes.contarNaoLidas(visualizador.id()));
    }
}
