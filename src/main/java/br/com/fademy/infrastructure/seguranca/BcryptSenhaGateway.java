package br.com.fademy.infrastructure.seguranca;

import br.com.fademy.application.gateway.SenhaGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptSenhaGateway implements SenhaGateway {

    private final BCryptPasswordEncoder bcrypt;

    public BcryptSenhaGateway(@Value("${fademy.bcrypt-rounds}") int rodadas) {
        this.bcrypt = new BCryptPasswordEncoder(rodadas);
    }

    @Override
    public String gerarHash(String senha) {
        return bcrypt.encode(senha);
    }

    @Override
    public boolean confere(String senha, String hash) {
        return bcrypt.matches(senha, hash);
    }
}
