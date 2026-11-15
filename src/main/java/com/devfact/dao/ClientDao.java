package com.devfact.dao;

import com.devfact.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux clients. */
public class ClientDao {

    private static final String SELECT_BASE =
            "SELECT c.id, c.name, c.contact_name, c.email, c.phone, c.address, c.city, c.tax_id, "
                    + "c.notes, c.active, c.created_at, "
                    + "(SELECT count(*) FROM document d WHERE d.client_id = c.id) AS doc_count "
                    + "FROM client c ";

    public List<Client> findAll(Connection connection, String search, boolean activeOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1 = 1 ");
        List<Object> parameters = new ArrayList<>();
        if (activeOnly) {
            sql.append("AND c.active = true ");
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c.name ILIKE ? OR coalesce(c.email, '') ILIKE ? OR coalesce(c.city, '') ILIKE ? ")
                    .append("OR coalesce(c.tax_id, '') ILIKE ? OR coalesce(c.phone, '') ILIKE ?) ");
            String like = "%" + search.trim() + "%";
            for (int i = 0; i < 5; i++) {
                parameters.add(like);
            }
        }
        sql.append("ORDER BY c.name ASC");

        List<Client> clients = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    clients.add(map(rs));
                }
            }
        }
        return clients;
    }

    public Optional<Client> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BASE + "WHERE c.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.<Client>empty();
            }
        }
    }

    public long insert(Connection connection, Client client) throws SQLException {
        String sql = "INSERT INTO client (name, contact_name, email, phone, address, city, tax_id, notes, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, client.getName());
            statement.setString(2, client.getContactName());
            statement.setString(3, client.getEmail());
            statement.setString(4, client.getPhone());
            statement.setString(5, client.getAddress());
            statement.setString(6, client.getCity());
            statement.setString(7, client.getTaxId());
            statement.setString(8, client.getNotes());
            statement.setBoolean(9, client.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    client.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Identifiant du client non généré");
    }

    public void update(Connection connection, Client client) throws SQLException {
        String sql = "UPDATE client SET name = ?, contact_name = ?, email = ?, phone = ?, address = ?, "
                + "city = ?, tax_id = ?, notes = ?, active = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, client.getName());
            statement.setString(2, client.getContactName());
            statement.setString(3, client.getEmail());
            statement.setString(4, client.getPhone());
            statement.setString(5, client.getAddress());
            statement.setString(6, client.getCity());
            statement.setString(7, client.getTaxId());
            statement.setString(8, client.getNotes());
            statement.setBoolean(9, client.isActive());
            statement.setLong(10, client.getId());
            statement.executeUpdate();
        }
    }

    public void setActive(Connection connection, long id, boolean active) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE client SET active = ? WHERE id = ?")) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM client WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public int countDocuments(Connection connection, long clientId) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT count(*) FROM document WHERE client_id = ?")) {
            statement.setLong(1, clientId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            if (value instanceof Long) {
                statement.setLong(i + 1, (Long) value);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
        }
    }

    private static Client map(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getLong("id"));
        client.setName(rs.getString("name"));
        client.setContactName(rs.getString("contact_name"));
        client.setEmail(rs.getString("email"));
        client.setPhone(rs.getString("phone"));
        client.setAddress(rs.getString("address"));
        client.setCity(rs.getString("city"));
        client.setTaxId(rs.getString("tax_id"));
        client.setNotes(rs.getString("notes"));
        client.setActive(rs.getBoolean("active"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        client.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        client.setDocumentCount(rs.getInt("doc_count"));
        return client;
    }
}
