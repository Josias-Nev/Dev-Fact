package com.devfact.security;

import com.devfact.model.Role;

import java.io.Serializable;

/** Utilisateur connecté, conservé en session HTTP. */
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String fullName;
    private final Role role;

    public UserSession(Long userId, String username, String fullName, Role role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName == null || fullName.isEmpty() ? username : fullName;
        this.role = role == null ? Role.COMMERCIAL : role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public String getRoleLabel() {
        return role.getLabel();
    }

    /** Propriété « admin » utilisée dans les JSP : ${user.admin}. */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Initiales affichées dans l'en-tête. */
    public String getInitials() {
        String source = fullName == null ? "" : fullName.trim();
        if (source.isEmpty()) {
            return "?";
        }
        String[] parts = source.split("\\s+");
        StringBuilder initials = new StringBuilder();
        initials.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) {
            initials.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return initials.toString();
    }
}
