package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.TransacaoGateway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringTransacaoGateway implements TransacaoGateway {

    private final TransactionTemplate transacao;

    public SpringTransacaoGateway(TransactionTemplate transacao) {
        this.transacao = transacao;
    }

    @Override
    public <T> T executar(Supplier<T> acao) {
        return transacao.execute(status -> acao.get());
    }
}
