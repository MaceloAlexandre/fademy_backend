package br.com.fademy.application.gateway;

import java.util.Optional;

public interface AvatarPadraoGateway {

    Optional<String> criar(String nome);
}
