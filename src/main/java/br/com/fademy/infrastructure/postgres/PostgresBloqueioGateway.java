package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.BloqueioGateway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
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

    @Override
    public void bloquear(String bloqueadorId, String bloqueadoId) {
        jdbc.sql("""
                        insert into bloqueio (id, bloqueador_id, bloqueado_id, criado_em)
                        values (:id, :bloqueador, :bloqueado, :agora)
                        on conflict (bloqueador_id, bloqueado_id) do nothing
                        """)
                .param("id", Ids.novo())
                .param("bloqueador", UUID.fromString(bloqueadorId))
                .param("bloqueado", UUID.fromString(bloqueadoId))
                .param("agora", Timestamp.from(Instant.now()))
                .update();
    }

    @Override
    public void desbloquear(String bloqueadorId, String bloqueadoId) {
        Optional<UUID> bloqueador = Ids.ler(bloqueadorId);
        Optional<UUID> bloqueado = Ids.ler(bloqueadoId);
        if (bloqueador.isPresent() && bloqueado.isPresent()) {
            jdbc.sql("delete from bloqueio where bloqueador_id = :bloqueador and bloqueado_id = :bloqueado")
                    .param("bloqueador", bloqueador.get())
                    .param("bloqueado", bloqueado.get())
                    .update();
        }
    }
}
