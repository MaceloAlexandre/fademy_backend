package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.BloqueioGateway;
import br.com.fademy.application.gateway.ConversaGateway;
import br.com.fademy.application.gateway.MensagemGateway;
import br.com.fademy.application.gateway.NotificacaoGateway;
import br.com.fademy.application.gateway.TempoRealGateway;
import br.com.fademy.application.usecase.ArquivoEnviado;
import br.com.fademy.application.usecase.MensagensUseCase;
import br.com.fademy.application.usecase.Pagina;
import br.com.fademy.domain.conversa.Conversa;
import br.com.fademy.domain.conversa.Mensagem;
import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.midia.Imagem;
import br.com.fademy.domain.notificacao.Notificacao;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.usuario.Visualizador;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MensagensService implements MensagensUseCase {

    private static final int MAXIMO_DE_IMAGENS = 4;

    private final ConversaGateway conversas;
    private final MensagemGateway mensagens;
    private final BloqueioGateway bloqueios;
    private final ArmazenamentoGateway armazenamento;
    private final NotificacaoGateway notificacoes;
    private final TempoRealGateway tempoReal;

    public MensagensService(ConversaGateway conversas, MensagemGateway mensagens, BloqueioGateway bloqueios,
                            ArmazenamentoGateway armazenamento, NotificacaoGateway notificacoes,
                            TempoRealGateway tempoReal) {
        this.conversas = conversas;
        this.mensagens = mensagens;
        this.bloqueios = bloqueios;
        this.armazenamento = armazenamento;
        this.notificacoes = notificacoes;
        this.tempoReal = tempoReal;
    }

    @Override
    public Mensagem enviar(Visualizador visualizador, String conversaId, String texto, List<ArquivoEnviado> imagens) {
        if (imagens.size() > MAXIMO_DE_IMAGENS) {
            throw new DadosInvalidosException("File upload error");
        }
        Conversa conversa = daConversa(visualizador, conversaId);
        String destinatario = conversa.outroMembro(visualizador.id());
        // Depois de um bloqueio, a conversa não aceita mais mensagens (SPEC D10).
        if (bloqueios.existeEntre(visualizador.id(), destinatario)) {
            throw new AcessoNegadoException("Forbidden");
        }
        Instant agora = Instant.now();
        Mensagem rascunho = Mensagem.escrever(conversa.id(), visualizador.id(), texto, imagens.size(), agora);
        if (imagens.stream().anyMatch(arquivo -> !Imagem.aceita(arquivo.tipo()))) {
            throw new DadosInvalidosException("Invalid file type");
        }

        List<String> midias = new ArrayList<>();
        for (ArquivoEnviado arquivo : imagens) {
            Imagem imagem = new Imagem(arquivo.conteudo(), arquivo.tipo());
            midias.add(armazenamento.enviar(imagem.novaChave("messages"), imagem));
        }
        Mensagem mensagem = mensagens.criar(rascunho.comMidias(midias));
        conversas.registrarAtividade(conversa.id(), agora);

        notificacoes.criar(Notificacao.nova(
                TipoNotificacao.MESSAGE_RECEIVED, destinatario, visualizador.id(), conversa.id(), agora));
        tempoReal.novaMensagem(destinatario, mensagem);
        return mensagem;
    }

    @Override
    public Pagina<Mensagem> listar(Visualizador visualizador, String conversaId, String cursor, String limite) {
        Conversa conversa = daConversa(visualizador, conversaId);
        int tamanho = Limites.ler(limite, 20, 50);
        return Pagina.de(mensagens.daConversa(conversa.id(), cursor, tamanho + 1), tamanho, Mensagem::id);
    }

    private Conversa daConversa(Visualizador visualizador, String conversaId) {
        Conversa conversa = conversas.buscarPorId(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversation not found"));
        conversa.garantirMembro(visualizador.id());
        return conversa;
    }
}
