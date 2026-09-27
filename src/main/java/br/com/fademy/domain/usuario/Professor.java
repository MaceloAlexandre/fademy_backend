package br.com.fademy.domain.usuario;

public class Professor extends MembroDaEquipe {

    public Professor(DadosUsuario dados) {
        super(dados);
    }

    @Override
    public Papel papel() {
        return Papel.PROFESSOR;
    }
}
