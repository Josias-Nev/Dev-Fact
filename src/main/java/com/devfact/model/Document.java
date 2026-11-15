package com.devfact.model;

import com.devfact.util.AmountToWordsFr;
import com.devfact.util.Dates;
import com.devfact.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Devis ou facture. */
public class Document {

    private Long id;
    private DocumentType type = DocumentType.DEVIS;
    private String number;
    private Long clientId;
    private String clientName;
    private DocumentStatus status = DocumentStatus.BROUILLON;
    private LocalDate issueDate = LocalDate.now();
    private LocalDate dueDate;
    private String subject;
    private String notes;
    private BigDecimal taxRate = new BigDecimal("18.00");
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String currency = Money.DEFAULT_CURRENCY;
    private Long sourceDocumentId;
    private String sourceDocumentNumber;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentLine> lines = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(Long sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }

    public String getSourceDocumentNumber() {
        return sourceDocumentNumber;
    }

    public void setSourceDocumentNumber(String sourceDocumentNumber) {
        this.sourceDocumentNumber = sourceDocumentNumber;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DocumentLine> getLines() {
        return lines;
    }

    public void setLines(List<DocumentLine> lines) {
        this.lines = lines == null ? new ArrayList<DocumentLine>() : lines;
    }

    // ------------------------------------------------------------------
    // Aides d'affichage utilisées par les JSP et les rapports
    // ------------------------------------------------------------------

    public boolean isNew() {
        return id == null;
    }

    public boolean isEditable() {
        return status == null || status.isEditable();
    }

    public boolean isQuote() {
        return type == DocumentType.DEVIS;
    }

    public String getIssueDateText() {
        return Dates.format(issueDate);
    }

    public String getDueDateText() {
        return Dates.format(dueDate);
    }

    public String getDueDateLabel() {
        return isQuote() ? "Valable jusqu'au" : "Échéance";
    }

    public String getSubtotalText() {
        return Money.formatAmount(subtotal, currency);
    }

    public String getTaxAmountText() {
        return Money.formatAmount(taxAmount, currency);
    }

    public String getTotalText() {
        return Money.formatAmount(total, currency);
    }

    public String getTotalWithSymbol() {
        return Money.format(total, currency);
    }

    public String getTaxRateText() {
        return taxRate == null ? "0" : taxRate.stripTrailingZeros().toPlainString();
    }

    /** Montant total en lettres (affiché sur la fiche et sur le PDF). */
    public String getAmountInWords() {
        String nature = type == null ? "document" : type.getLabel().toLowerCase(java.util.Locale.FRANCE);
        return "Arrêtée la présente " + nature + " à la somme de : "
                + AmountToWordsFr.inWords(total, currency);
    }

    public String getDisplayTitle() {
        return (type == null ? "Document" : type.getLabel()) + (number == null ? "" : " n° " + number);
    }
}
