package com.devfact.service;

import com.devfact.dao.SettingsDao;
import com.devfact.db.DataAccess;
import com.devfact.model.CompanySettings;
import com.devfact.util.Money;
import com.devfact.util.Texts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Paramètres de la société. */
public class SettingsService {

    private static final BigDecimal MAX_RATE = new BigDecimal("100");

    private final SettingsDao settingsDao = new SettingsDao();

    public CompanySettings load() {
        return DataAccess.read(connection -> settingsDao.load(connection));
    }

    public CompanySettings save(CompanySettings settings) {
        validate(settings);
        DataAccess.write(connection -> {
            settingsDao.save(connection, settings);
            return null;
        });
        return settings;
    }

    public void validate(CompanySettings settings) {
        List<String> errors = new ArrayList<>();
        settings.setCompanyName(Texts.trimToNull(settings.getCompanyName()));
        settings.setAddress(Texts.trimToNull(settings.getAddress()));
        settings.setCity(Texts.trimToNull(settings.getCity()));
        settings.setPhone(Texts.trimToNull(settings.getPhone()));
        settings.setEmail(Texts.trimToNull(settings.getEmail()));
        settings.setTaxId(Texts.trimToNull(settings.getTaxId()));
        settings.setBankDetails(Texts.trimToNull(settings.getBankDetails()));
        settings.setFooterNote(Texts.trimToNull(settings.getFooterNote()));
        settings.setQuotePrefix(Texts.trimToNull(settings.getQuotePrefix()));
        settings.setInvoicePrefix(Texts.trimToNull(settings.getInvoicePrefix()));

        if (settings.getCompanyName() == null) {
            errors.add("La raison sociale de la société est obligatoire.");
        }
        if (settings.getQuotePrefix() == null) {
            errors.add("Le préfixe des devis est obligatoire.");
        }
        if (settings.getInvoicePrefix() == null) {
            errors.add("Le préfixe des factures est obligatoire.");
        }
        if (settings.getDefaultTaxRate() == null
                || settings.getDefaultTaxRate().signum() < 0
                || settings.getDefaultTaxRate().compareTo(MAX_RATE) > 0) {
            errors.add("Le taux de TVA par défaut doit être comprise entre 0 et 100 %.");
        }
        String currency = settings.getCurrency() == null
                ? Money.DEFAULT_CURRENCY : settings.getCurrency().trim().toUpperCase(Locale.ROOT);
        if (currency.length() != 3) {
            errors.add("Le code devise doit comporter 3 lettres (ex. XOF, EUR).");
        }
        settings.setCurrency(currency);
        if (settings.getPaymentTermsDays() < 0 || settings.getPaymentTermsDays() > 365) {
            errors.add("Le délai de paiement doit être compris entre 0 et 365 jours.");
        }
        if (settings.getQuoteValidityDays() < 0 || settings.getQuoteValidityDays() > 365) {
            errors.add("La validité des devis doit être comprise entre 0 et 365 jours.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
