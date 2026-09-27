package br.com.fademy.domain.social;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.notificacao.TipoNotificacao;
import br.com.fademy.domain.usuario.Usuario;

import java.time.Instant;

/**
 * Um usuário seguindo outro (SPEC RN21–RN24).
 */
public class Seguir {

    private final String id;
    private final String seguidorId;
    private final String seguidoId;
    private StatusSeguir status;
    private final Instant criadoEm;

    public Seguir(String id, String seguidorId, String seguidoId, StatusSeguir status, Instant criadoEm) {
        this.id = id;
        this.seguidorId = seguidorId;
        this.seguidoId = seguidoId;
        this.status = status;
        this.criadoEm = criadoEm;
    }

    /** Perfil privado recebe um pedido; perfil público é seguido na hora (SPEC RN21). */
    public static Seguir solicitar(Usuario seguidor, Usuario alvo, Instant agora) {
        return new Seguir(null, seguidor.id(), alvo.id(),
                alvo.privado() ? StatusSeguir.PENDING : StatusSeguir.ACCEPTED, agora);
    }

    /** O aviso que o seguido recebe: novo seguidor ou pedido para seguir (SPEC RN34). */
    public TipoNotificacao notificacaoParaOSeguido() {
        return status == StatusSeguir.ACCEPTED ? TipoNotificacao.NEW_FOLLOWER : TipoNotificacao.FOLLOW_REQUEST;
    }

    /** Só o seguido responde a um pedido (SPEC RN23, RN24). */
    public void garantirQueEhOSeguido(String usuarioId) {
        if (!seguidoId.equals(usuarioId)) {
            throw new AcessoNegadoException("Forbidden");
        }
    }

    /** O seguido aceita um pedido pendente (SPEC RN23). */
    public void aceitarPor(String usuarioId) {
        garantirQueEhOSeguido(usuarioId);
        if (status != StatusSeguir.PENDING) {
            throw new DadosInvalidosException("No pending request");
        }
        status = StatusSeguir.ACCEPTED;
    }

    public String id() {
        return id;
    }

    public String seguidorId() {
        return seguidorId;
    }

    public String seguidoId() {
        return seguidoId;
    }

    public StatusSeguir status() {
        return status;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}
