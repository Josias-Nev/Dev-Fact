package com.devfact.db;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Source de données minimale basée sur le pilote JDBC.
 * <p>
 * Elle n'est utilisée que si la source de données JNDI de WildFly n'est pas configurée :
 * l'application reste ainsi déployable « telle quelle » (Docker, tests, poste de dev).
 */
public class DriverManagerDataSource implements DataSource {

    private final String url;
    private final String user;
    private final String password;
    private final Driver driver;
    private final Properties properties = new Properties();

    public DriverManagerDataSource(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.driver = loadDriver();
        if (user != null) {
            properties.setProperty("user", user);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
    }

    private static Driver loadDriver() {
        try {
            Class<?> clazz = Class.forName("org.postgresql.Driver");
            return (Driver) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getUrl() {
        return url;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (driver == null) {
            throw new SQLException("Pilote JDBC PostgreSQL introuvable dans le classpath");
        }
        Connection connection = driver.connect(url, properties);
        if (connection == null) {
            throw new SQLException("Connexion impossible à " + url + " (URL non acceptée par le pilote)");
        }
        return connection;
    }

    @Override
    public Connection getConnection(String username, String pwd) throws SQLException {
        if (driver == null) {
            throw new SQLException("Pilote JDBC PostgreSQL introuvable dans le classpath");
        }
        Properties props = new Properties();
        props.setProperty("user", username == null ? "" : username);
        props.setProperty("password", pwd == null ? "" : pwd);
        Connection connection = driver.connect(url, props);
        if (connection == null) {
            throw new SQLException("Connexion impossible à " + url);
        }
        return connection;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        // non supporté
    }

    @Override
    public void setLoginTimeout(int seconds) {
        // non supporté
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("com.devfact.db");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Pas de wrapper pour " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
