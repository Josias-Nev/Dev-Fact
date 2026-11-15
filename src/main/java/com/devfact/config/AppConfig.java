package com.devfact.config;

/**
 * Configuration de l'application.
 * <p>
 * Chaque valeur peut être fournie par une propriété système ({@code -Ddevfact.db.url=...})
 * ou par une variable d'environnement ({@code DEV_FACT_DB_URL}), utile avec Docker.
 */
public final class AppConfig {

    public static final String DEFAULT_JNDI_NAME = "java:jboss/datasources/DevFactDS";
    public static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/devfact";

    private AppConfig() {
    }

    /** Nom JNDI de la source de données WildFly. */
    public static String jndiName() {
        return get("devfact.ds", "DEV_FACT_DS", DEFAULT_JNDI_NAME);
    }

    /** URL JDBC utilisée lorsque la source de données JNDI est absente. */
    public static String dbUrl() {
        return get("devfact.db.url", "DEV_FACT_DB_URL", DEFAULT_DB_URL);
    }

    public static String dbUser() {
        return get("devfact.db.user", "DEV_FACT_DB_USER", "devfact");
    }

    public static String dbPassword() {
        return get("devfact.db.password", "DEV_FACT_DB_PASSWORD", "devfact");
    }

    public static String applicationName() {
        return get("devfact.app.name", "DEV_FACT_APP_NAME", "Dev-Fact");
    }

    private static String get(String systemProperty, String environmentVariable, String fallback) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environmentVariable);
        }
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
