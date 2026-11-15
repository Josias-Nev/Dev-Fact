package com.devfact.security;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Module de connexion JAAS personnalisé : il vérifie les identifiants dans la table
 * {@code app_user} de PostgreSQL (empreintes PBKDF2).
 */
public class DevFactLoginModule implements LoginModule {

    private static final Logger LOG = Logger.getLogger(DevFactLoginModule.class.getName());

    private Subject subject;
    private CallbackHandler callbackHandler;
    private DevFactPrincipal principal;
    private boolean succeeded;
    private boolean committed;
    private AuthService authService;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.authService = createAuthService();
    }

    /**
     * Fabrique du service d'authentification ; point d'extension utilisé par les tests
     * pour substituer la vérification en base.
     */
    protected AuthService createAuthService() {
        return new AuthService();
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("Aucun CallbackHandler fourni");
        }
        NameCallback nameCallback = new NameCallback("Identifiant : ");
        PasswordCallback passwordCallback = new PasswordCallback("Mot de passe : ", false);
        Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ex) {
            throw new LoginException("Erreur de lecture des identifiants : " + ex.getMessage());
        } catch (UnsupportedCallbackException ex) {
            throw new LoginException("Callback non supporté : " + ex.getMessage());
        }

        String username = nameCallback.getName();
        char[] password = passwordCallback.getPassword();
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            throw new FailedLoginException("Identifiant ou mot de passe manquant");
        }

        try {
            this.principal = authService.verifyCredentials(username.trim(), new String(password));
            this.succeeded = true;
            return true;
        } catch (AuthService.CredentialException ex) {
            LOG.info("Échec d'authentification pour " + username + " : " + ex.getMessage());
            FailedLoginException failure = new FailedLoginException("Identifiant ou mot de passe incorrect");
            failure.initCause(ex);
            throw failure;
        } finally {
            passwordCallback.clearPassword();
        }
    }

    @Override
    public boolean commit() {
        if (!succeeded || principal == null) {
            return false;
        }
        if (subject.getPrincipals().contains(principal)) {
            committed = true;
            return true;
        }
        subject.getPrincipals().add(principal);
        committed = true;
        return true;
    }

    @Override
    public boolean abort() {
        if (!succeeded) {
            return false;
        }
        succeeded = false;
        principal = null;
        return true;
    }

    @Override
    public boolean logout() {
        if (committed && principal != null) {
            subject.getPrincipals().remove(principal);
        }
        committed = false;
        succeeded = false;
        principal = null;
        return true;
    }
}
