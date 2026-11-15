package com.devfact.model;

import com.devfact.util.Money;

import java.math.BigDecimal;

/** Informations de la société émettrice, affichées en en-tête des documents. */
public class CompanySettings {

    private String companyName = "";
    private String address;
    private String city;
    private String phone;
    private String email;
    private String taxId;
    private String bankDetails;
    private String currency = Money.DEFAULT_CURRENCY;
    private BigDecimal defaultTaxRate = new BigDecimal("18.00");
    private String quotePrefix = "DEV";
    private String invoicePrefix = "FAC";
    private int paymentTermsDays = 30;
    private int quoteValidityDays = 30;
    private String footerNote;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getBankDetails() {
        return bankDetails;
    }

    public void setBankDetails(String bankDetails) {
        this.bankDetails = bankDetails;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getDefaultTaxRate() {
        return defaultTaxRate;
    }

    public void setDefaultTaxRate(BigDecimal defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate;
    }

    public String getQuotePrefix() {
        return quotePrefix;
    }

    public void setQuotePrefix(String quotePrefix) {
        this.quotePrefix = quotePrefix;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public void setInvoicePrefix(String invoicePrefix) {
        this.invoicePrefix = invoicePrefix;
    }

    public int getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public void setPaymentTermsDays(int paymentTermsDays) {
        this.paymentTermsDays = paymentTermsDays;
    }

    public int getQuoteValidityDays() {
        return quoteValidityDays;
    }

    public void setQuoteValidityDays(int quoteValidityDays) {
        this.quoteValidityDays = quoteValidityDays;
    }

    public String getFooterNote() {
        return footerNote;
    }

    public void setFooterNote(String footerNote) {
        this.footerNote = footerNote;
    }

    public String getCurrencySymbol() {
        return Money.symbolFor(currency);
    }

    /** Nombre de décimales de la devise (0 pour le franc CFA). */
    public int getCurrencyDecimals() {
        return Money.decimalsFor(currency);
    }

    public String getAddressLine() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isEmpty()) {
            sb.append(address);
        }
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(city);
        }
        return sb.toString();
    }
}
