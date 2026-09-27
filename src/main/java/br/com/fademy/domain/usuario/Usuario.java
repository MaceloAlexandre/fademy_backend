package br.com.fademy.domain.usuario;

import java.time.Instant;

/**
 * Usuário da Fademy. Cada papel é uma subclasse e responde o que pode fazer (SPEC RN05).
 * Por padrão, nenhuma permissão especial: as subclasses liberam o que o papel permite.
 */
public abstract class Usuario {

    private final String id;
    private final String instituicaoId;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private String avatarUrl;
    private boolean privado;
    private final boolean ativo;
    private final Instant criadoEm;

    protected Usuario(DadosUsuario dados) {
        this.id = dados.id();
        this.instituicaoId = dados.instituicaoId();
        this.nome = dados.nome();
        this.email = dados.email();
        this.senhaHash = dados.senhaHash();
        this.avatarUrl = dados.avatarUrl();
        this.privado = dados.privado();
        this.ativo = dados.ativo();
        this.criadoEm = dados.criadoEm();
    }

    public abstract Papel papel();

    /** Coordenação, professores, funcionários e administradores. */
    public boolean ehDaEquipe() {
        return false;
    }

    public boolean podeGerarConvite() {
        return false;
    }

    /** Só administrador gera convite para outro administrador. */
    public boolean podeGerarConvitePara(Papel alvo) {
        return podeGerarConvite() && alvo != Papel.ADMIN;
    }

    public boolean podePublicarLink() {
        return false;
    }

    public boolean podeAcessarPainelAdmin() {
        return false;
    }

    /**
     * Perfil privado só mostra números e posts ao dono e a quem o segue com follow aceito
     * (SPEC RN17, RN31).
     *
     * @param seguidoPeloVisualizador o visualizador segue este usuário com follow aceito
     */
    public boolean mostraConteudoPara(String visualizadorId, boolean seguidoPeloVisualizador) {
        return !privado || id.equals(visualizadorId) || seguidoPeloVisualizador;
    }

    public void definirPrivacidade(boolean privado) {
        this.privado = privado;
    }

    public void trocarAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /** Usuários de instituições diferentes não se enxergam (SPEC RN01). */
    public boolean mesmaInstituicao(Usuario outro) {
        return instituicaoId.equals(outro.instituicaoId);
    }

    public String id() {
        return id;
    }

    public String instituicaoId() {
        return instituicaoId;
    }

    public String nome() {
        return nome;
    }

    public String email() {
        return email;
    }

    public String senhaHash() {
        return senhaHash;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public boolean privado() {
        return privado;
    }

    public boolean ativo() {
        return ativo;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}
