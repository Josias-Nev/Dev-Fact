package com.devfact.db;

import com.devfact.config.AppConfig;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Point d'entrée unique pour l'accès à PostgreSQL. */
public final class DataAccess {

    private static final Logger LOG = Logger.getLogger(DataAccess.class.getName());

    private static volatile DataSource dataSource;
    private static volatile String description = "(non initialisé)";

    private DataAccess() {
    }

    /** Travail SQL exécuté avec une connexion fournie. */
    public interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    /** Initialise la source de données (JNDI WildFly, sinon JDBC direct). */
    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }
        String jndiName = AppConfig.jndiName();
        try {
            InitialContext context = new InitialContext();
            Object looked = context.lookup(jndiName);
            context.close();
            if (looked instanceof DataSource) {
                dataSource = (DataSource) looked;
                description = "JNDI " + jndiName;
                LOG.info("Source de données initialisée : " + description);
                return;
            }
        } catch (NamingException ex) {
            LOG.log(Level.FINE, "Source de données JNDI indisponible : " + jndiName, ex);
        }
        DriverManagerDataSource fallback =
                new DriverManagerDataSource(AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword());
        dataSource = fallback;
        description = "JDBC direct " + fallback.getUrl();
        LOG.info("Source de données initialisée : " + description);
    }

    /** Réinitialise (tests, rechargement). */
    public static synchronized void reset() {
        dataSource = null;
        description = "(non initialisé)";
    }

    public static String describe() {
        return description;
    }

    public static DataSource dataSource() {
        if (dataSource == null) {
            init();
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    /** Exécute un travail en lecture (connexion fermée automatiquement). */
    public static <T> T read(SqlWork<T> work) {
        try (Connection connection = getConnection()) {
            return work.apply(connection);
        } catch (SQLException ex) {
            throw new DataAccessException("Erreur lors de la lecture des données", ex);
        }
    }

    /** Exécute un travail en écriture dans une transaction. */
    public static <T> T write(SqlWork<T> work) {
        try (Connection connection = getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException ex) {
                rollback(connection);
                throw new DataAccessException("Erreur lors de l'enregistrement : " + ex.getMessage(), ex);
            } catch (RuntimeException ex) {
                rollback(connection);
                throw ex;
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // rien à faire
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Connexion à la base impossible", ex);
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Échec du rollback", ex);
        }
    }

    /** Vérifie que la base répond (utilisé au démarrage). */
    public static boolean checkHealth() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Base de données injoignable : " + ex.getMessage(), ex);
            return false;
        }
    }
}
