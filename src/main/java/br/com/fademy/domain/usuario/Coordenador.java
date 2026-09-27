package br.com.fademy.domain.usuario;

public class Coordenador extends MembroDaEquipe {

    public Coordenador(DadosUsuario dados) {
        super(dados);
    }

    @Override
    public Papel papel() {
        return Papel.COORDENADOR;
    }
}
