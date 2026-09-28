package br.com.fademy.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

public class PostgresAdapter implements DataBaseAdapter {

    private final String url;
    private final String usuario;
    private final String senha;
    private final int timeoutSegundos;

    private HikariDataSource dataSource;

    public PostgresAdapter(String url, String usuario, String senha, int timeoutSegundos) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
        this.timeoutSegundos = timeoutSegundos;
    }

    @Override
    public void conectar() {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(url);
        hikari.setUsername(usuario);
        hikari.setPassword(senha);
        hikari.setConnectionTimeout(TimeUnit.SECONDS.toMillis(timeoutSegundos));
        dataSource = new HikariDataSource(hikari);
    }

    @Override
    public void desconectar() {
        dataSource.close();
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
