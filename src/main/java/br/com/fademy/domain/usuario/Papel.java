package br.com.fademy.domain.usuario;

import br.com.fademy.domain.excecao.DadosInvalidosException;

/**
 * Papéis de usuário (SPEC RN04). O papel vem do convite e não muda depois.
 */
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

    /** Como o papel aparece na API e no banco. */
    public String texto() {
        return texto;
    }

    /** Vagas máximas de um convite com este papel-alvo (SPEC RN06). */
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

    /** Fábrica: monta a subclasse de {@link Usuario} que corresponde a este papel. */
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
