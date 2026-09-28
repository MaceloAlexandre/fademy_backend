package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.usecase.VerImagemUseCase;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import br.com.fademy.domain.midia.Imagem;

public class VerImagemService implements VerImagemUseCase {

    private final ArmazenamentoGateway armazenamento;

    public VerImagemService(ArmazenamentoGateway armazenamento) {
        this.armazenamento = armazenamento;
    }

    @Override
    public Imagem executar(String chave) {
        return armazenamento.ler(chave)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Image not found"));
    }
}
