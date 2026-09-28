package br.com.fademy.domain.usuario;

import br.com.fademy.domain.excecao.DadosInvalidosException;

public enum Papel {

    ADMIN("admin", 10),
    COORDENADOR("coordenador", 10),
    PROFESSOR("professor", 10),
    FUNCIONARIO("funcionario", 10),
    ALUNO("aluno", 50);

    private final String texto;
    private final int vagasMaximasPorConvite;

    Papel(String texto, int vagasMaximasPorConvite) {
        this.texto = texto;
        this.vagasMaximasPorConvite = vagasMaximasPorConvite;
    }

    public String texto() {
        return texto;
    }

    public int vagasMaximasPorConvite() {
        return vagasMaximasPorConvite;
    }

    public static Papel deTexto(String texto) {
        for (Papel papel : values()) {
            if (papel.texto.equals(texto)) {
                return papel;
            }
        }
        throw new DadosInvalidosException("Invalid roleTarget");
    }

    public Usuario criarUsuario(DadosUsuario dados) {
        return switch (this) {
            case ADMIN -> new Administrador(dados);
            case COORDENADOR -> new Coordenador(dados);
            case PROFESSOR -> new Professor(dados);
            case FUNCIONARIO -> new Funcionario(dados);
            case ALUNO -> new Aluno(dados);
        };
    }
}
