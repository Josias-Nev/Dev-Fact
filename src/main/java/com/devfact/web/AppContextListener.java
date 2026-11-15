package com.devfact.web;

import com.devfact.db.DataAccess;
import com.devfact.report.ReportService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Initialisation au démarrage : source de données, contrôle de santé, modèle PDF. */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOG.info("Démarrage de Dev-Fact");
        try {
            DataAccess.init();
            boolean available = DataAccess.checkHealth();
            AppStatus.setDatabaseAvailable(available);
            if (available) {
                LOG.info("Base de données joignable (" + DataAccess.describe() + ")");
                new ReportService().warmUp();
            } else {
                AppStatus.setMessage("La base de données est injoignable : vérifiez la configuration JDBC.");
                LOG.severe("Base de données injoignable (" + DataAccess.describe() + ")");
            }
        } catch (RuntimeException ex) {
            AppStatus.setDatabaseAvailable(false);
            AppStatus.setMessage("Initialisation impossible : " + ex.getMessage());
            LOG.log(Level.SEVERE, "Initialisation de l'application en échec", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOG.info("Arrêt de Dev-Fact");
    }
}
