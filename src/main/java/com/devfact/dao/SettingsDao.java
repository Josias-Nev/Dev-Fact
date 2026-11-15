package com.devfact.dao;

import com.devfact.model.CompanySettings;
import com.devfact.util.Money;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Paramètres de la société. */
public class SettingsDao {

    public CompanySettings load(Connection connection) throws SQLException {
        String sql = "SELECT company_name, address, city, phone, email, tax_id, bank_details, currency, "
                + "default_tax_rate, quote_prefix, invoice_prefix, payment_terms_days, quote_validity_days, "
                + "footer_note FROM company_settings WHERE id = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (!rs.next()) {
                return new CompanySettings();
            }
            CompanySettings settings = new CompanySettings();
            settings.setCompanyName(rs.getString("company_name"));
            settings.setAddress(rs.getString("address"));
            settings.setCity(rs.getString("city"));
            settings.setPhone(rs.getString("phone"));
            settings.setEmail(rs.getString("email"));
            settings.setTaxId(rs.getString("tax_id"));
            settings.setBankDetails(rs.getString("bank_details"));
            settings.setCurrency(rs.getString("currency"));
            BigDecimal taxRate = rs.getBigDecimal("default_tax_rate");
            settings.setDefaultTaxRate(taxRate == null ? new BigDecimal("18.00") : taxRate);
            settings.setQuotePrefix(rs.getString("quote_prefix"));
            settings.setInvoicePrefix(rs.getString("invoice_prefix"));
            settings.setPaymentTermsDays(rs.getInt("payment_terms_days"));
            settings.setQuoteValidityDays(rs.getInt("quote_validity_days"));
            settings.setFooterNote(rs.getString("footer_note"));
            return settings;
        }
    }

    public void save(Connection connection, CompanySettings settings) throws SQLException {
        String sql = "INSERT INTO company_settings (id, company_name, address, city, phone, email, tax_id, "
                + "bank_details, currency, default_tax_rate, quote_prefix, invoice_prefix, payment_terms_days, "
                + "quote_validity_days, footer_note) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (id) DO UPDATE SET company_name = EXCLUDED.company_name, address = EXCLUDED.address, "
                + "city = EXCLUDED.city, phone = EXCLUDED.phone, email = EXCLUDED.email, tax_id = EXCLUDED.tax_id, "
                + "bank_details = EXCLUDED.bank_details, currency = EXCLUDED.currency, "
                + "default_tax_rate = EXCLUDED.default_tax_rate, quote_prefix = EXCLUDED.quote_prefix, "
                + "invoice_prefix = EXCLUDED.invoice_prefix, payment_terms_days = EXCLUDED.payment_terms_days, "
                + "quote_validity_days = EXCLUDED.quote_validity_days, footer_note = EXCLUDED.footer_note";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, settings.getCompanyName());
            statement.setString(2, settings.getAddress());
            statement.setString(3, settings.getCity());
            statement.setString(4, settings.getPhone());
            statement.setString(5, settings.getEmail());
            statement.setString(6, settings.getTaxId());
            statement.setString(7, settings.getBankDetails());
            statement.setString(8, settings.getCurrency() == null ? Money.DEFAULT_CURRENCY : settings.getCurrency());
            statement.setBigDecimal(9, settings.getDefaultTaxRate());
            statement.setString(10, settings.getQuotePrefix());
            statement.setString(11, settings.getInvoicePrefix());
            statement.setInt(12, settings.getPaymentTermsDays());
            statement.setInt(13, settings.getQuoteValidityDays());
            statement.setString(14, settings.getFooterNote());
            statement.executeUpdate();
        }
    }
}
