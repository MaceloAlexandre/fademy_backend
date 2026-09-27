package br.com.fademy.domain.usuario;

/**
 * Coordenador, professor, funcionário e administrador: podem convidar e publicar links.
 */
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
