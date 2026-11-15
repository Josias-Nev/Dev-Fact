package com.devfact.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Erreur de validation métier. Non contrôlée : elle annule automatiquement la transaction
 * en cours et est interceptée par les servlets pour réafficher le formulaire.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    public ValidationException(String error) {
        this(Collections.singletonList(error));
    }

    public ValidationException(List<String> errors) {
        super(String.join(" ", errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public ValidationException(String error, Throwable cause) {
        super(error, cause);
        this.errors = Collections.singletonList(error);
    }

    public List<String> getErrors() {
        return errors;
    }

    public static ValidationException of(String... errors) {
        return new ValidationException(Arrays.asList(errors));
    }
}
