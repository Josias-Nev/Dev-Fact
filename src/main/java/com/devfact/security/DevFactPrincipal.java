package com.devfact.security;

import com.devfact.model.Role;

import java.io.Serializable;
import java.security.Principal;

/** Principal JAAS représentant un utilisateur authentifié. */
public class DevFactPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String fullName;
    private final Role role;

    public DevFactPrincipal(Long userId, String username, String fullName, Role role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role == null ? Role.COMMERCIAL : role;
    }

    @Override
    public String getName() {
        return username;
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

    /** Rôle au format attendu par les contraintes de sécurité des conteneurs Java EE. */
    public String getRoleName() {
        return role.name();
    }

    @Override
    public String toString() {
        return "DevFactPrincipal{" + username + ", " + role + '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DevFactPrincipal)) {
            return false;
        }
        DevFactPrincipal that = (DevFactPrincipal) other;
        return username != null && username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return username == null ? 0 : username.hashCode();
    }
}
