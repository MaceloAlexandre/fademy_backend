package br.com.fademy.domain.usuario;

/**
 * Aluno: não convida nem publica links.
 */
public class Aluno extends Usuario {

    public Aluno(DadosUsuario dados) {
        super(dados);
    }

    @Override
    public Papel papel() {
        return Papel.ALUNO;
    }
}
