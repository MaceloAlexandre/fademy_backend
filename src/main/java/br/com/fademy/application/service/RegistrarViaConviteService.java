package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.SenhaGateway;
import br.com.fademy.application.gateway.TransacaoGateway;
import br.com.fademy.application.gateway.UsuarioGateway;
import br.com.fademy.application.usecase.RegistrarViaConviteUseCase;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.usuario.DadosUsuario;
import br.com.fademy.domain.usuario.Usuario;

import java.time.Instant;
import java.util.stream.Stream;

public class RegistrarViaConviteService implements RegistrarViaConviteUseCase {

    private final ConviteGateway convites;
    private final UsuarioGateway usuarios;
    private final ArmazenamentoGateway armazenamento;
    private final SenhaGateway senhas;
    private final TransacaoGateway transacao;
    private final NotificacaoGateway notificacoes;

    public RegistrarViaConviteService(ConviteGateway convites, UsuarioGateway usuarios,
                                      ArmazenamentoGateway armazenamento, SenhaGateway senhas,
                                      TransacaoGateway transacao, NotificacaoGateway notificacoes) {
        this.convites = convites;
        this.usuarios = usuarios;
        this.armazenamento = armazenamento;
        this.senhas = senhas;
        this.transacao = transacao;
        this.notificacoes = notificacoes;
    }

    @Override
    public Usuario executar(Cadastro c) {
        if (Stream.of(c.token(), c.nome(), c.email(), c.senha(), c.avatarUrl(), c.avatarChave())
                .anyMatch(campo -> campo == null || campo.isEmpty())) {
            throw new DadosInvalidosException(
                    "token, name, email, password, avatarUrl, and avatarKey are required");
        }
        Instant agora = Instant.now();

        Convite convite = convites.buscarPorToken(c.token())
                .orElseThrow(() -> new DadosInvalidosException("Invalid invite token"));
        convite.garantirValido(agora);

        if (!armazenamento.existe(c.avatarChave())) {
            throw new DadosInvalidosException("Profile photo not found in storage — please upload a photo first");
        }

        // Instituição e papel vêm do convite, nunca do corpo da requisição.
        Usuario novo = convite.papelAlvo().criarUsuario(new DadosUsuario(
                null, convite.instituicaoId(), c.nome(), c.email(), senhas.gerarHash(c.senha()), c.avatarChave(),
                false, true, agora));

        // Usuário e vaga juntos (RN46); a notificação vem depois do commit (RN47).
        Usuario criado = transacao.executar(() -> {
            Usuario usuario = usuarios.criar(novo);
            if (!convites.consumirVaga(convite.id())) {
                throw new DadosInvalidosException("Invite has no remaining slots");
            }
            return usuario;
        });

        notificacoes.criar(Notificacao.nova(
                TipoNotificacao.INVITE_RECEIVED, convite.convidadoPorId(), criado.id(), criado.id(), agora));
        return criado;
    }
}
