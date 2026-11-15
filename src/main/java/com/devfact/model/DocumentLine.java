package com.devfact.model;

import com.devfact.util.Money;

import java.math.BigDecimal;

/** Ligne d'un devis ou d'une facture. */
public class DocumentLine {

    private Long id;
    private Long documentId;
    private int position;
    private String description;
    private BigDecimal quantity = BigDecimal.ONE;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;
    private String currency = Money.DEFAULT_CURRENCY;

    public DocumentLine() {
    }

    public DocumentLine(String description, BigDecimal quantity, BigDecimal unitPrice) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    // ------------------------------------------------------------------
    // Aides d'affichage
    // ------------------------------------------------------------------

    public String getQuantityText() {
        BigDecimal value = quantity == null ? BigDecimal.ZERO : quantity;
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    public String getUnitPriceText() {
        return Money.formatAmount(unitPrice, currency);
    }

    public String getLineTotalText() {
        return Money.formatAmount(lineTotal, currency);
    }
}
