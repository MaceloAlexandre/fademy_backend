package br.com.fademy.domain.usuario;

public abstract class MembroDaEquipe extends Usuario {

    protected MembroDaEquipe(DadosUsuario dados) {
        super(dados);
    }

    @Override
    public boolean ehDaEquipe() {
        return true;
    }

    @Override
    public boolean podeGerarConvite() {
        return true;
    }

    @Override
    public boolean podePublicarLink() {
        return true;
    }
}
