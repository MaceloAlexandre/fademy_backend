package br.com.fademy.infrastructure.postgres;

import br.com.fademy.application.gateway.SaudeGateway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class PostgresSaudeGateway implements SaudeGateway {

    private static final int TIMEOUT_SEGUNDOS = 2;

    private final DataSource dataSource;

    public PostgresSaudeGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean estaNoAr() {
        try (Connection conexao = dataSource.getConnection()) {
            return conexao.isValid(TIMEOUT_SEGUNDOS);
        } catch (SQLException e) {
            return false;
        }
    }
}
