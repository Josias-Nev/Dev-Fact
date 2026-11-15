package com.devfact.service;

import java.math.BigDecimal;

/** Totaux calculés d'un document : sous-total HT, TVA, total TTC. */
public class Totals {

    private final BigDecimal subtotal;
    private final BigDecimal taxAmount;
    private final BigDecimal total;

    public Totals(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total) {
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.total = total;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
