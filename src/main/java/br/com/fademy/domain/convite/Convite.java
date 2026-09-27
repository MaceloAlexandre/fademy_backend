package br.com.fademy.domain.convite;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.usuario.Papel;
import br.com.fademy.domain.usuario.Usuario;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Link de convite com papel-alvo, vagas e validade (SPEC RN10–RN13).
 */
public class Convite {

    private static final Duration VALIDADE_PADRAO = Duration.ofDays(7);

    private final String id;
    private final String instituicaoId;
    private final String convidadoPorId;
    private final Papel papelAlvo;
    private final String token;
    private final int maxUsos;
    private final int usos;
    private StatusConvite status;
    private final Instant expiraEm;
    private final Instant criadoEm;

    public Convite(String id, String instituicaoId, String convidadoPorId, Papel papelAlvo, String token,
                   int maxUsos, int usos, StatusConvite status, Instant expiraEm, Instant criadoEm) {
        this.id = id;
        this.instituicaoId = instituicaoId;
        this.convidadoPorId = convidadoPorId;
        this.papelAlvo = papelAlvo;
        this.token = token;
        this.maxUsos = maxUsos;
        this.usos = usos;
        this.status = status;
        this.expiraEm = expiraEm;
        this.criadoEm = criadoEm;
    }

    /**
     * Novo convite gerado pelo autor, na instituição dele (SPEC RN10). Quem chama já garantiu
     * que o autor pode gerar convites (RN08).
     *
     * @param expiraEm opcional; sem ele, o convite vale 7 dias
     */
    public static Convite gerar(Usuario autor, String papelAlvo, Integer maxUsos, Instant expiraEm, Instant agora) {
        if (papelAlvo == null || maxUsos == null) {
            throw new DadosInvalidosException("roleTarget and maxUses are required");
        }
        Papel alvo = Papel.deTexto(papelAlvo);
        if (!autor.podeGerarConvitePara(alvo)) {
            throw new AcessoNegadoException("Only admin can generate admin invites");
        }
        if (maxUsos < 1) {
            throw new DadosInvalidosException("maxUses must be at least 1");
        }
        if (maxUsos > alvo.vagasMaximasPorConvite()) {
            throw new DadosInvalidosException("maxUses exceeds cap of " + alvo.vagasMaximasPorConvite()
                    + " for roleTarget " + alvo.texto());
        }
        return new Convite(null, autor.instituicaoId(), autor.id(), alvo, UUID.randomUUID().toString(),
                maxUsos, 0, StatusConvite.ACTIVE,
                expiraEm != null ? expiraEm : agora.plus(VALIDADE_PADRAO), agora);
    }

    public void revogar() {
        status = StatusConvite.REVOKED;
    }

    public boolean daInstituicao(String instituicaoId) {
        return this.instituicaoId.equals(instituicaoId);
    }

    public String id() {
        return id;
    }

    public String instituicaoId() {
        return instituicaoId;
    }

    public String convidadoPorId() {
        return convidadoPorId;
    }

    public Papel papelAlvo() {
        return papelAlvo;
    }

    public String token() {
        return token;
    }

    public int maxUsos() {
        return maxUsos;
    }

    public int usos() {
        return usos;
    }

    public StatusConvite status() {
        return status;
    }

    public Instant expiraEm() {
        return expiraEm;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}
