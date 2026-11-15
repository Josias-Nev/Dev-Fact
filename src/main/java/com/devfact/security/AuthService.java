package com.devfact.security;

import com.devfact.dao.UserDao;
import com.devfact.db.DataAccess;
import com.devfact.db.DataAccessException;
import com.devfact.model.UserAccount;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentification de l'application.
 * <p>
 * Le parcours réel passe par JAAS : un {@link javax.security.auth.login.LoginContext}
 * instancie {@link DevFactLoginModule}, qui interroge PostgreSQL via {@link #verifyCredentials}.
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    /** Échec de vérification des identifiants. */
    public static class CredentialException extends Exception {

        private static final long serialVersionUID = 1L;
        private final boolean technical;

        public CredentialException(String message) {
            this(message, null, false);
        }

        public CredentialException(String message, Throwable cause) {
            this(message, cause, false);
        }

        public CredentialException(String message, Throwable cause, boolean technical) {
            super(message, cause);
            this.technical = technical;
        }

        public boolean isTechnical() {
            return technical;
        }
    }

    /** Vérifie un couple identifiant / mot de passe (appelé par le module JAAS). */
    public DevFactPrincipal verifyCredentials(String username, String password) throws CredentialException {
        UserAccount account;
        try {
            account = DataAccess.read(connection -> new UserDao().findByUsername(connection, username))
                    .orElse(null);
        } catch (DataAccessException ex) {
            throw new CredentialException("Base de données indisponible", ex, true);
        }
        if (account == null) {
            throw new CredentialException("Utilisateur inconnu");
        }
        if (!account.isEnabled()) {
            throw new CredentialException("Compte désactivé");
        }
        if (!PasswordHasher.matches(password, account.getPasswordHash())) {
            throw new CredentialException("Mot de passe invalide");
        }
        return new DevFactPrincipal(account.getId(), account.getUsername(), account.getFullName(), account.getRole());
    }

    /**
     * Authentifie l'utilisateur via JAAS et renvoie la session applicative.
     *
     * @throws AuthenticationException identifiants invalides ou erreur technique
     */
    public UserSession authenticate(String username, String password) throws AuthenticationException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Veuillez saisir votre identifiant et votre mot de passe.");
        }
        Subject subject = new Subject();
        LoginContext context;
        try {
            context = new LoginContext(DevFactJaasConfig.CONTEXT_NAME, subject,
                    new DevFactCallbackHandler(username, password), new DevFactJaasConfig());
            context.login();
        } catch (LoginException ex) {
            CredentialException credential = findCredentialException(ex);
            if (credential != null && credential.isTechnical()) {
                throw new AuthenticationException("La base de données est momentanément indisponible.", credential, true);
            }
            throw new AuthenticationException("Identifiant ou mot de passe incorrect.");
        }

        try {
            DevFactPrincipal principal = findPrincipal(subject);
            if (principal == null) {
                throw new AuthenticationException("Authentification incomplète : aucun principal JAAS.");
            }
            touchLastLogin(principal.getUserId());
            return new UserSession(principal.getUserId(), principal.getUsername(),
                    principal.getFullName(), principal.getRole());
        } finally {
            try {
                context.logout();
            } catch (LoginException ex) {
                LOG.log(Level.FINE, "Déconnexion JAAS : {0}", ex.getMessage());
            }
        }
    }

    private CredentialException findCredentialException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CredentialException) {
                return (CredentialException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    private DevFactPrincipal findPrincipal(Subject subject) {
        Set<DevFactPrincipal> principals = subject.getPrincipals(DevFactPrincipal.class);
        return principals.isEmpty() ? null : principals.iterator().next();
    }

    private void touchLastLogin(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            DataAccess.write(connection -> {
                new UserDao().touchLastLogin(connection, userId);
                return null;
            });
        } catch (DataAccessException ex) {
            LOG.log(Level.WARNING, "Mise à jour de la date de connexion impossible", ex);
        }
    }
}
