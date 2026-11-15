package com.devfact.model;

import java.util.Locale;

/** Rôles applicatifs. */
public enum Role {

    ADMIN("Administrateur"),
    COMMERCIAL("Commercial");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Role fromCode(String code) {
        if (code == null) {
            return COMMERCIAL;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (Role role : values()) {
            if (role.name().equals(normalized)) {
                return role;
            }
        }
        return COMMERCIAL;
    }
}
