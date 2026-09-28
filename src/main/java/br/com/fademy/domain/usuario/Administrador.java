package br.com.fademy.domain.usuario;

public class Administrador extends MembroDaEquipe {

    public Administrador(DadosUsuario dados) {
        super(dados);
    }

    @Override
    public Papel papel() {
        return Papel.ADMIN;
    }

    @Override
    public boolean podeGerarConvitePara(Papel alvo) {
        return true;
    }

    @Override
    public boolean podeAcessarPainelAdmin() {
        return true;
    }
}
