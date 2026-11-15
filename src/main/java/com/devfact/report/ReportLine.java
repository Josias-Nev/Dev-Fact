package com.devfact.report;

import com.devfact.model.DocumentLine;

/** Ligne de document exposée au modèle JasperReports. */
public class ReportLine {

    private final String designation;
    private final String quantityText;
    private final String unitPriceText;
    private final String totalText;

    public ReportLine(DocumentLine line) {
        this.designation = line.getDescription();
        this.quantityText = line.getQuantityText();
        this.unitPriceText = line.getUnitPriceText();
        this.totalText = line.getLineTotalText();
    }

    public String getDesignation() {
        return designation;
    }

    public String getQuantityText() {
        return quantityText;
    }

    public String getUnitPriceText() {
        return unitPriceText;
    }

    public String getTotalText() {
        return totalText;
    }
}
