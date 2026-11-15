package com.devfact.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Compteur de numérotation des documents.
 * L'incrément est atomique (ON CONFLICT ... RETURNING), donc sûr en concurrence.
 */
public class SequenceDao {

    public int nextValue(Connection connection, String docType, int period) throws SQLException {
        String sql = "INSERT INTO document_sequence (doc_type, period, last_value) VALUES (?, ?, 1) "
                + "ON CONFLICT (doc_type, period) "
                + "DO UPDATE SET last_value = document_sequence.last_value + 1 "
                + "RETURNING last_value";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, docType);
            statement.setInt(2, period);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Compteur de numérotation indisponible pour " + docType + "/" + period);
    }
}
