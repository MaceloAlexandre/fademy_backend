package br.com.fademy.application.usecase;

import br.com.fademy.domain.conteudo.Comentario;
import br.com.fademy.domain.usuario.Usuario;

public record ComentarioDetalhado(Comentario comentario, Usuario autor) {
}
