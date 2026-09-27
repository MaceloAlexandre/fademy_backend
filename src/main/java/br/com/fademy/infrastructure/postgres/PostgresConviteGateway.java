package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.ConviteGateway;
import br.com.fademy.domain.convite.Convite;
import br.com.fademy.domain.convite.StatusConvite;
import br.com.fademy.domain.usuario.Papel;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresConviteGateway implements ConviteGateway {

    private static final RowMapper<Convite> CONVITE = (rs, n) -> new Convite(
            rs.getString("id"),
            rs.getString("instituicao_id"),
            rs.getString("convidado_por_id"),
            Papel.deTexto(rs.getString("papel_alvo")),
            rs.getString("token"),
            rs.getInt("max_usos"),
            rs.getInt("usos"),
            StatusConvite.deTexto(rs.getString("status")),
            rs.getTimestamp("expira_em").toInstant(),
            rs.getTimestamp("criado_em").toInstant());

    private final JdbcClient jdbc;

    public PostgresConviteGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Convite criar(Convite c) {
        UUID id = Ids.novo();
        jdbc.sql("""
                        insert into convite (id, instituicao_id, convidado_por_id, papel_alvo, token, max_usos, usos,
                                             status, expira_em, criado_em)
                        values (:id, :instituicaoId, :convidadoPorId, :papelAlvo, :token, :maxUsos, :usos,
                                :status, :expiraEm, :criadoEm)
                        """)
                .param("id", id)
                .param("instituicaoId", UUID.fromString(c.instituicaoId()))
                .param("convidadoPorId", UUID.fromString(c.convidadoPorId()))
                .param("papelAlvo", c.papelAlvo().texto())
                .param("token", c.token())
                .param("maxUsos", c.maxUsos())
                .param("usos", c.usos())
                .param("status", c.status().texto())
                .param("expiraEm", Timestamp.from(c.expiraEm()))
                .param("criadoEm", Timestamp.from(c.criadoEm()))
                .update();
        return new Convite(id.toString(), c.instituicaoId(), c.convidadoPorId(), c.papelAlvo(), c.token(),
                c.maxUsos(), c.usos(), c.status(), c.expiraEm(), c.criadoEm());
    }

    @Override
    public Optional<Convite> buscarPorToken(String token) {
        return jdbc.sql("select * from convite where token = :token")
                .param("token", token)
                .query(CONVITE)
                .optional();
    }

    @Override
    public void atualizarStatus(Convite convite) {
        jdbc.sql("update convite set status = :status where id = :id")
                .param("status", convite.status().texto())
                .param("id", UUID.fromString(convite.id()))
                .update();
    }

    @Override
    public boolean consumirVaga(String conviteId) {
        // A condição no WHERE torna o consumo atômico: com dois cadastros simultâneos, o segundo
        // UPDATE espera o primeiro terminar, reavalia a condição e não encontra mais vaga.
        int atualizados = jdbc.sql("""
                        update convite
                        set usos = usos + 1,
                            status = case when usos + 1 >= max_usos then 'expired' else status end
                        where id = :id and status = 'active' and usos < max_usos
                        """)
                .param("id", UUID.fromString(conviteId))
                .update();
        return atualizados == 1;
    }
}
