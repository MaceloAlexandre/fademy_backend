package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.SeguirGateway;
import br.com.fademy.domain.excecao.ConflitoException;
import br.com.fademy.domain.social.Seguir;
import br.com.fademy.domain.social.StatusSeguir;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresSeguirGateway implements SeguirGateway {

    static final RowMapper<Seguir> SEGUIR = (rs, n) -> new Seguir(
            rs.getString("id"),
            rs.getString("seguidor_id"),
            rs.getString("seguido_id"),
            StatusSeguir.deTexto(rs.getString("status")),
            rs.getTimestamp("criado_em").toInstant());

    private final JdbcClient jdbc;

    public PostgresSeguirGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Seguir> buscar(String seguidorId, String seguidoId) {
        Optional<UUID> seguidor = Ids.ler(seguidorId);
        Optional<UUID> seguido = Ids.ler(seguidoId);
        if (seguidor.isEmpty() || seguido.isEmpty()) {
            return Optional.empty();
        }
        return jdbc.sql("select * from seguir where seguidor_id = :seguidor and seguido_id = :seguido")
                .param("seguidor", seguidor.get())
                .param("seguido", seguido.get())
                .query(SEGUIR)
                .optional();
    }

    @Override
    public Optional<Seguir> buscarPorId(String id) {
        return Ids.ler(id).flatMap(uuid -> jdbc.sql("select * from seguir where id = :id")
                .param("id", uuid)
                .query(SEGUIR)
                .optional());
    }

    @Override
    public List<Seguir> buscarPorIds(Collection<String> ids) {
        List<UUID> validos = ids.stream().flatMap(id -> Ids.ler(id).stream()).toList();
        if (validos.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select * from seguir where id in (:ids)").param("ids", validos).query(SEGUIR).list();
    }

    @Override
    public void atualizarStatus(Seguir seguir) {
        jdbc.sql("update seguir set status = :status where id = :id")
                .param("status", seguir.status().texto())
                .param("id", UUID.fromString(seguir.id()))
                .update();
    }

    @Override
    public void removerPorId(String id) {
        jdbc.sql("delete from seguir where id = :id").param("id", UUID.fromString(id)).update();
    }

    @Override
    public boolean seguemUmAoOutro(String usuarioA, String usuarioB) {
        return jdbc.sql("""
                        select count(*) = 2 from seguir
                        where status = 'accepted'
                          and ((seguidor_id = :a and seguido_id = :b) or (seguidor_id = :b and seguido_id = :a))
                        """)
                .param("a", UUID.fromString(usuarioA))
                .param("b", UUID.fromString(usuarioB))
                .query(Boolean.class)
                .single();
    }

    @Override
    public long contarSeguidores(String usuarioId) {
        return jdbc.sql("select count(*) from seguir where seguido_id = :id and status = 'accepted'")
                .param("id", UUID.fromString(usuarioId))
                .query(Long.class)
                .single();
    }

    @Override
    public long contarSeguindo(String usuarioId) {
        return jdbc.sql("select count(*) from seguir where seguidor_id = :id and status = 'accepted'")
                .param("id", UUID.fromString(usuarioId))
                .query(Long.class)
                .single();
    }

    @Override
    public void removerEntre(String usuarioA, String usuarioB) {
        jdbc.sql("""
                        delete from seguir
                        where (seguidor_id = :a and seguido_id = :b) or (seguidor_id = :b and seguido_id = :a)
                        """)
                .param("a", UUID.fromString(usuarioA))
                .param("b", UUID.fromString(usuarioB))
                .update();
    }

    @Override
    public Seguir criar(Seguir s) {
        UUID id = Ids.novo();
        try {
            jdbc.sql("""
                            insert into seguir (id, seguidor_id, seguido_id, status, criado_em)
                            values (:id, :seguidor, :seguido, :status, :criadoEm)
                            """)
                    .param("id", id)
                    .param("seguidor", UUID.fromString(s.seguidorId()))
                    .param("seguido", UUID.fromString(s.seguidoId()))
                    .param("status", s.status().texto())
                    .param("criadoEm", Timestamp.from(s.criadoEm()))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new ConflitoException("Already following or request pending");
        }
        return new Seguir(id.toString(), s.seguidorId(), s.seguidoId(), s.status(), s.criadoEm());
    }

    @Override
    public void remover(String seguidorId, String seguidoId) {
        Optional<UUID> seguidor = Ids.ler(seguidorId);
        Optional<UUID> seguido = Ids.ler(seguidoId);
        if (seguidor.isPresent() && seguido.isPresent()) {
            jdbc.sql("delete from seguir where seguidor_id = :seguidor and seguido_id = :seguido")
                    .param("seguidor", seguidor.get())
                    .param("seguido", seguido.get())
                    .update();
        }
    }
}
