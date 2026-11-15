package com.devfact.dao;

import com.devfact.model.Document;
import com.devfact.model.DocumentLine;
import com.devfact.model.DocumentStatus;
import com.devfact.model.DocumentType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux devis et factures. */
public class DocumentDao {

    private static final String SELECT_BASE =
            "SELECT d.id, d.doc_type, d.number, d.client_id, d.status, d.issue_date, d.due_date, d.subject, "
                    + "d.notes, d.tax_rate, d.subtotal, d.tax_amount, d.total, d.currency, d.source_document_id, "
                    + "d.created_by, d.created_at, d.updated_at, c.name AS client_name, "
                    + "src.number AS source_number "
                    + "FROM document d "
                    + "JOIN client c ON c.id = d.client_id "
                    + "LEFT JOIN document src ON src.id = d.source_document_id ";

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    public Optional<Document> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BASE + "WHERE d.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.<Document>empty();
            }
        }
    }

    public List<Document> search(Connection connection, DocumentQuery query) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE);
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, query);
        sql.append("ORDER BY d.issue_date DESC, d.id DESC LIMIT ? OFFSET ?");
        parameters.add(query.getPageSize());
        parameters.add(query.getOffset());

        List<Document> documents = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    documents.add(map(rs));
                }
            }
        }
        return documents;
    }

    public int count(Connection connection, DocumentQuery query) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT count(*) FROM document d JOIN client c ON c.id = d.client_id ");
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, query);
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<DocumentLine> findLines(Connection connection, long documentId) throws SQLException {
        String sql = "SELECT l.id, l.document_id, l.position, l.description, l.quantity, l.unit_price, l.line_total, "
                + "d.currency FROM document_line l JOIN document d ON d.id = l.document_id "
                + "WHERE l.document_id = ? ORDER BY l.position, l.id";
        List<DocumentLine> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, documentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    DocumentLine line = new DocumentLine();
                    line.setId(rs.getLong("id"));
                    line.setDocumentId(rs.getLong("document_id"));
                    line.setPosition(rs.getInt("position"));
                    line.setDescription(rs.getString("description"));
                    line.setQuantity(rs.getBigDecimal("quantity"));
                    line.setUnitPrice(rs.getBigDecimal("unit_price"));
                    line.setLineTotal(rs.getBigDecimal("line_total"));
                    line.setCurrency(rs.getString("currency"));
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    public Optional<Long> findInvoiceIdForQuote(Connection connection, long quoteId) throws SQLException {
        String sql = "SELECT id FROM document WHERE source_document_id = ? AND doc_type = 'FACTURE' LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, quoteId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.<Long>empty();
            }
        }
    }

    // ------------------------------------------------------------------
    // Écriture
    // ------------------------------------------------------------------

    public long insert(Connection connection, Document document) throws SQLException {
        String sql = "INSERT INTO document (doc_type, number, client_id, status, issue_date, due_date, subject, "
                + "notes, tax_rate, subtotal, tax_amount, total, currency, source_document_id, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, document.getType().name());
            statement.setString(2, document.getNumber());
            statement.setLong(3, document.getClientId());
            statement.setString(4, document.getStatus().name());
            statement.setDate(5, toSqlDate(document.getIssueDate()));
            statement.setDate(6, toSqlDate(document.getDueDate()));
            statement.setString(7, document.getSubject());
            statement.setString(8, document.getNotes());
            statement.setBigDecimal(9, document.getTaxRate());
            statement.setBigDecimal(10, document.getSubtotal());
            statement.setBigDecimal(11, document.getTaxAmount());
            statement.setBigDecimal(12, document.getTotal());
            statement.setString(13, document.getCurrency());
            setNullableLong(statement, 14, document.getSourceDocumentId());
            setNullableLong(statement, 15, document.getCreatedBy());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    document.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Identifiant du document non généré");
    }

    public void updateHeader(Connection connection, Document document) throws SQLException {
        String sql = "UPDATE document SET client_id = ?, status = ?, issue_date = ?, due_date = ?, subject = ?, "
                + "notes = ?, tax_rate = ?, subtotal = ?, tax_amount = ?, total = ?, currency = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, document.getClientId());
            statement.setString(2, document.getStatus().name());
            statement.setDate(3, toSqlDate(document.getIssueDate()));
            statement.setDate(4, toSqlDate(document.getDueDate()));
            statement.setString(5, document.getSubject());
            statement.setString(6, document.getNotes());
            statement.setBigDecimal(7, document.getTaxRate());
            statement.setBigDecimal(8, document.getSubtotal());
            statement.setBigDecimal(9, document.getTaxAmount());
            statement.setBigDecimal(10, document.getTotal());
            statement.setString(11, document.getCurrency());
            statement.setLong(12, document.getId());
            statement.executeUpdate();
        }
    }

    public void updateStatus(Connection connection, long id, DocumentStatus status) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("UPDATE document SET status = ? WHERE id = ?")) {
            statement.setString(1, status.name());
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void deleteLines(Connection connection, long documentId) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM document_line WHERE document_id = ?")) {
            statement.setLong(1, documentId);
            statement.executeUpdate();
        }
    }

    public void insertLines(Connection connection, long documentId, List<DocumentLine> lines) throws SQLException {
        String sql = "INSERT INTO document_line (document_id, position, description, quantity, unit_price, line_total) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int position = 1;
            for (DocumentLine line : lines) {
                line.setPosition(position++);
                line.setDocumentId(documentId);
                statement.setLong(1, documentId);
                statement.setInt(2, line.getPosition());
                statement.setString(3, line.getDescription());
                statement.setBigDecimal(4, line.getQuantity());
                statement.setBigDecimal(5, line.getUnitPrice());
                statement.setBigDecimal(6, line.getLineTotal());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM document WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void appendFilters(StringBuilder sql, List<Object> parameters, DocumentQuery query) {
        sql.append("WHERE d.doc_type = ? ");
        parameters.add(query.getType().name());
        if (query.getStatus() != null) {
            sql.append("AND d.status = ? ");
            parameters.add(query.getStatus().name());
        }
        if (query.getClientId() != null) {
            sql.append("AND d.client_id = ? ");
            parameters.add(query.getClientId());
        }
        if (query.getText() != null) {
            sql.append("AND (d.number ILIKE ? OR c.name ILIKE ? OR coalesce(d.subject, '') ILIKE ?) ");
            String like = "%" + query.getText() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            if (value instanceof Long) {
                statement.setLong(i + 1, (Long) value);
            } else if (value instanceof Integer) {
                statement.setInt(i + 1, (Integer) value);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static Date toSqlDate(java.time.LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private static java.time.LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static Document map(ResultSet rs) throws SQLException {
        Document document = new Document();
        document.setId(rs.getLong("id"));
        document.setType(DocumentType.fromCode(rs.getString("doc_type")));
        document.setNumber(rs.getString("number"));
        document.setClientId(rs.getLong("client_id"));
        document.setClientName(rs.getString("client_name"));
        DocumentStatus status = DocumentStatus.fromCode(rs.getString("status"));
        document.setStatus(status == null ? DocumentStatus.BROUILLON : status);
        document.setIssueDate(toLocalDate(rs.getDate("issue_date")));
        document.setDueDate(toLocalDate(rs.getDate("due_date")));
        document.setSubject(rs.getString("subject"));
        document.setNotes(rs.getString("notes"));
        BigDecimal taxRate = rs.getBigDecimal("tax_rate");
        document.setTaxRate(taxRate == null ? BigDecimal.ZERO : taxRate);
        document.setSubtotal(rs.getBigDecimal("subtotal"));
        document.setTaxAmount(rs.getBigDecimal("tax_amount"));
        document.setTotal(rs.getBigDecimal("total"));
        document.setCurrency(rs.getString("currency"));
        long sourceId = rs.getLong("source_document_id");
        document.setSourceDocumentId(rs.wasNull() ? null : sourceId);
        long createdBy = rs.getLong("created_by");
        document.setCreatedBy(rs.wasNull() ? null : createdBy);
        Timestamp createdAt = rs.getTimestamp("created_at");
        document.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        document.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        document.setSourceDocumentNumber(rs.getString("source_number"));
        return document;
    }
}
