package br.com.fademy.domain.usuario;

import java.time.Instant;

public abstract class Usuario {

    private final String id;
    private final String instituicaoId;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private String avatarChave;
    private boolean privado;
    private final boolean ativo;
    private final Instant criadoEm;

    protected Usuario(DadosUsuario dados) {
        this.id = dados.id();
        this.instituicaoId = dados.instituicaoId();
        this.nome = dados.nome();
        this.email = dados.email();
        this.senhaHash = dados.senhaHash();
        this.avatarChave = dados.avatarChave();
        this.privado = dados.privado();
        this.ativo = dados.ativo();
        this.criadoEm = dados.criadoEm();
    }

    public abstract Papel papel();

    public boolean ehDaEquipe() {
        return false;
    }

    public boolean podeGerarConvite() {
        return false;
    }

    public boolean podeGerarConvitePara(Papel alvo) {
        return podeGerarConvite() && alvo != Papel.ADMIN;
    }

    public boolean podePublicarLink() {
        return false;
    }

    public boolean podeAcessarPainelAdmin() {
        return false;
    }

    public boolean mostraConteudoPara(String visualizadorId, boolean seguidoPeloVisualizador) {
        return !privado || id.equals(visualizadorId) || seguidoPeloVisualizador;
    }

    public void definirPrivacidade(boolean privado) {
        this.privado = privado;
    }

    public void trocarAvatar(String avatarChave) {
        this.avatarChave = avatarChave;
    }

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

    public String avatarChave() {
        return avatarChave;
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
