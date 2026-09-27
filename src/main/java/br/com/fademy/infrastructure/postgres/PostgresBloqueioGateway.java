package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.BloqueioGateway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresBloqueioGateway implements BloqueioGateway {

    private final JdbcClient jdbc;

    public PostgresBloqueioGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existeEntre(String usuarioA, String usuarioB) {
        Optional<UUID> a = Ids.ler(usuarioA);
        Optional<UUID> b = Ids.ler(usuarioB);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return jdbc.sql("""
                        select exists (select 1 from bloqueio
                                       where (bloqueador_id = :a and bloqueado_id = :b)
                                          or (bloqueador_id = :b and bloqueado_id = :a))
                        """)
                .param("a", a.get())
                .param("b", b.get())
                .query(Boolean.class)
                .single();
    }
}
