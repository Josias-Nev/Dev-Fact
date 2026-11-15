package com.devfact.security;

/** Échec d'authentification (identifiants invalides, compte désactivé, base indisponible). */
public class AuthenticationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean technical;

    public AuthenticationException(String message) {
        this(message, null, false);
    }

    public AuthenticationException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public AuthenticationException(String message, Throwable cause, boolean technical) {
        super(message, cause);
        this.technical = technical;
    }

    /** Vrai si l'échec vient d'un problème technique (base indisponible) et non des identifiants. */
    public boolean isTechnical() {
        return technical;
    }
}
