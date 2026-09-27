package br.com.fademy.domain.usuario;

public class Funcionario extends MembroDaEquipe {

    public Funcionario(DadosUsuario dados) {
        super(dados);
    }

    @Override
    public Papel papel() {
        return Papel.FUNCIONARIO;
    }
}
