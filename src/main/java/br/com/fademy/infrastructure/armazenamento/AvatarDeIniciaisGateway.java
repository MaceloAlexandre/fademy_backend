package br.com.fademy.infrastructure.armazenamento;

import br.com.fademy.application.gateway.ArmazenamentoGateway;
import br.com.fademy.application.gateway.AvatarPadraoGateway;
import br.com.fademy.domain.midia.Imagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AvatarDeIniciaisGateway implements AvatarPadraoGateway {

    private static final Logger log = LoggerFactory.getLogger(AvatarDeIniciaisGateway.class);

    private final ArmazenamentoGateway armazenamento;

    public AvatarDeIniciaisGateway(ArmazenamentoGateway armazenamento) {
        this.armazenamento = armazenamento;
    }

    @Override
    public Optional<String> criar(String nome) {
        Imagem imagem = new Imagem(AvatarDeIniciais.png(nome, AvatarDeIniciais.corPara(nome)), "image/png");
        String chave = imagem.novaChave("avatars");
        try {
            armazenamento.enviar(chave, imagem);
            return Optional.of(chave);
        } catch (RuntimeException e) {
            // Sem armazenamento na subida: segue sem foto, em vez de derrubar a aplicação.
            log.warn("Não foi possível gravar o avatar padrão de {}", nome, e);
            return Optional.empty();
        }
    }
}
