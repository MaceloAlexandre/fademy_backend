package br.com.fademy.application.service;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.midia.Imagem;

public class EnviarAvatarService implements EnviarAvatarUseCase {

    private final ArmazenamentoGateway armazenamento;

    public EnviarAvatarService(ArmazenamentoGateway armazenamento) {
        this.armazenamento = armazenamento;
    }

    @Override
    public String executar(byte[] conteudo, String tipo) {
        if (conteudo == null) {
            throw new DadosInvalidosException("file is required");
        }
        if (!Imagem.aceita(tipo)) {
            throw new DadosInvalidosException("file must be image/jpeg, image/png, or image/webp");
        }
        Imagem imagem = new Imagem(conteudo, tipo);
        String chave = imagem.novaChave("avatars");
        armazenamento.enviar(chave, imagem);
        return chave;
    }
}
