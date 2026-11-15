package com.devfact.dao;

import com.devfact.model.Role;
import com.devfact.model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux comptes utilisateurs. */
public class UserDao {

    private static final String SELECT_BASE =
            "SELECT id, username, password_hash, full_name, email, role, enabled, created_at, last_login_at "
                    + "FROM app_user ";

    public Optional<UserAccount> findByUsername(Connection connection, String username) throws SQLException {
        if (username == null) {
            return Optional.empty();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement(SELECT_BASE + "WHERE lower(username) = lower(?)")) {
            statement.setString(1, username.trim());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.<UserAccount>empty();
            }
        }
    }

    public Optional<UserAccount> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BASE + "WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.<UserAccount>empty();
            }
        }
    }

    public List<UserAccount> findAll(Connection connection) throws SQLException {
        List<UserAccount> accounts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BASE + "ORDER BY username")) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(map(rs));
                }
            }
        }
        return accounts;
    }

    public void touchLastLogin(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("UPDATE app_user SET last_login_at = now() WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public long insert(Connection connection, UserAccount account) throws SQLException {
        String sql = "INSERT INTO app_user (username, password_hash, full_name, email, role, enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, account.getUsername());
            statement.setString(2, account.getPasswordHash());
            statement.setString(3, account.getFullName());
            statement.setString(4, account.getEmail());
            statement.setString(5, account.getRole() == null ? Role.COMMERCIAL.name() : account.getRole().name());
            statement.setBoolean(6, account.isEnabled());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    account.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Identifiant utilisateur non généré");
    }

    public void updatePassword(Connection connection, long id, String passwordHash) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("UPDATE app_user SET password_hash = ? WHERE id = ?")) {
            statement.setString(1, passwordHash);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void setEnabled(Connection connection, long id, boolean enabled) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("UPDATE app_user SET enabled = ? WHERE id = ?")) {
            statement.setBoolean(1, enabled);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    private static UserAccount map(ResultSet rs) throws SQLException {
        UserAccount account = new UserAccount();
        account.setId(rs.getLong("id"));
        account.setUsername(rs.getString("username"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setFullName(rs.getString("full_name"));
        account.setEmail(rs.getString("email"));
        account.setRole(Role.fromCode(rs.getString("role")));
        account.setEnabled(rs.getBoolean("enabled"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        account.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        account.setLastLoginAt(lastLogin == null ? null : lastLogin.toLocalDateTime());
        return account;
    }
}
