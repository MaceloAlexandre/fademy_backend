package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ConversaGateway;
import br.com.fademy.application.gateway.MensagemGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.ListarConversasUseCase;
import br.com.fademy.domain.conversa.Conversa;
import br.com.fademy.domain.usuario.Usuario;
import br.com.fademy.domain.usuario.Visualizador;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListarConversasService implements ListarConversasUseCase {

    private final UsuarioGateway usuarios;
    private final ConversaGateway conversas;
    private final MensagemGateway mensagens;

    public ListarConversasService(UsuarioGateway usuarios, ConversaGateway conversas, MensagemGateway mensagens) {
        this.usuarios = usuarios;
        this.conversas = conversas;
        this.mensagens = mensagens;
    }

    @Override
    public List<ConversaDetalhada> conversas(Visualizador visualizador) {
        List<Conversa> minhas = conversas.doUsuario(visualizador.id());
        Map<String, Usuario> membros = usuarios.buscarPorIds(
                        minhas.stream().flatMap(c -> c.membros().stream()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Usuario::id, Function.identity()));
        return minhas.stream().map(c -> new ConversaDetalhada(c,
                c.membros().stream().map(membros::get).filter(Objects::nonNull).toList(),
                mensagens.ultimaDe(c.id()).orElse(null))).toList();
    }

    @Override
    public List<Usuario> contatos(Visualizador visualizador) {
        return usuarios.seguidoresMutuos(usuarios.doVisualizador(visualizador));
    }
}
