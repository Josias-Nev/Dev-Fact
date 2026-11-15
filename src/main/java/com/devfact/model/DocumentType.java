package com.devfact.model;

import java.util.Locale;

/** Nature d'un document commercial. */
public enum DocumentType {

    DEVIS("Devis", "Devis", "DEV"),
    FACTURE("Facture", "Factures", "FAC");

    private final String label;
    private final String pluralLabel;
    private final String defaultPrefix;

    DocumentType(String label, String pluralLabel, String defaultPrefix) {
        this.label = label;
        this.pluralLabel = pluralLabel;
        this.defaultPrefix = defaultPrefix;
    }

    public String getLabel() {
        return label;
    }

    public String getPluralLabel() {
        return pluralLabel;
    }

    public String getDefaultPrefix() {
        return defaultPrefix;
    }

    public String getCode() {
        return name();
    }

    public static DocumentType fromCode(String code) {
        if (code == null) {
            return DEVIS;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (DocumentType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return DEVIS;
    }
}
