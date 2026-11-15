package com.devfact.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Statuts des devis et des factures, avec les transitions autorisées. */
public enum DocumentStatus {

    BROUILLON("Brouillon", "neutral"),
    ENVOYE("Envoyé", "info"),
    ACCEPTE("Accepté", "success"),
    REFUSE("Refusé", "danger"),
    FACTURE("Facturé", "brand"),
    EMISE("Émise", "info"),
    PAYEE("Payée", "success"),
    ANNULEE("Annulée", "danger");

    private final String label;
    private final String style;

    DocumentStatus(String label, String style) {
        this.label = label;
        this.style = style;
    }

    public String getLabel() {
        return label;
    }

    public String getStyle() {
        return style;
    }

    public String getCode() {
        return name();
    }

    public boolean isEditable() {
        return this == BROUILLON;
    }

    public boolean isFinal() {
        return this == REFUSE || this == FACTURE || this == PAYEE || this == ANNULEE;
    }

    public boolean belongsTo(DocumentType type) {
        return forType(type).contains(this);
    }

    public static List<DocumentStatus> forType(DocumentType type) {
        if (type == DocumentType.FACTURE) {
            return Collections.unmodifiableList(Arrays.asList(BROUILLON, EMISE, PAYEE, ANNULEE));
        }
        return Collections.unmodifiableList(Arrays.asList(BROUILLON, ENVOYE, ACCEPTE, REFUSE, FACTURE));
    }

    public boolean canMoveTo(DocumentStatus target) {
        if (target == null || target == this) {
            return false;
        }
        switch (this) {
            case BROUILLON:
                return target == ENVOYE || target == ACCEPTE || target == REFUSE || target == EMISE;
            case ENVOYE:
                return target == ACCEPTE || target == REFUSE;
            case ACCEPTE:
                return target == FACTURE;
            case EMISE:
                return target == PAYEE || target == ANNULEE;
            default:
                return false;
        }
    }

    public static DocumentStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (DocumentStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}
