package br.com.fademy.domain.usuario;

public class Aluno extends Usuario {

    public Aluno(DadosUsuario dados) {
        super(dados);
    }

    @Override
    public Papel papel() {
        return Papel.ALUNO;
    }
}
