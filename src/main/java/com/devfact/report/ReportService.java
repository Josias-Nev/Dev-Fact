package com.devfact.report;

import com.devfact.model.Client;
import com.devfact.model.CompanySettings;
import com.devfact.model.Document;
import com.devfact.model.DocumentLine;
import com.devfact.util.AmountToWordsFr;
import com.devfact.util.Dates;
import com.devfact.util.Money;
import com.devfact.util.Texts;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Génération des PDF (devis et factures) avec JasperReports.
 * <p>
 * Le modèle {@code /reports/document.jasper} (précompilé, livré dans le WAR) est chargé une seule
 * fois puis mis en cache ; à défaut, {@code /reports/document.jrxml} est compilé au démarrage.
 */
public class ReportService {

    private static final Logger LOG = Logger.getLogger(ReportService.class.getName());
    private static final String COMPILED_PATH = "/reports/document.jasper";
    private static final String SOURCE_PATH = "/reports/document.jrxml";

    private static volatile JasperReport compiledReport;

    /** Compile le modèle dès le démarrage pour échouer vite en cas de problème. */
    public void warmUp() {
        try {
            report();
            LOG.info("Modèle JasperReports chargé.");
        } catch (ReportException ex) {
            LOG.log(Level.SEVERE, "Compilation du modèle JasperReports impossible", ex);
        }
    }

    /** Rend le document au format PDF. */
    public byte[] renderPdf(Document document, List<DocumentLine> lines, Client client, CompanySettings settings) {
        try {
            Map<String, Object> parameters = buildParameters(document, client, settings);
            List<ReportLine> rows = new ArrayList<>();
            for (DocumentLine line : lines) {
                rows.add(new ReportLine(line));
            }
            JasperPrint print = JasperFillManager.fillReport(
                    report(), parameters, new JRBeanCollectionDataSource(rows));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException ex) {
            throw new ReportException("Génération du PDF impossible : " + ex.getMessage(), ex);
        }
    }

    /** Nom de fichier proposé au téléchargement (ex. FAC-2026-0001.pdf). */
    public String fileName(Document document) {
        String base = document.getNumber() == null ? "document" : document.getNumber();
        return base.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
    }

    private JasperReport report() {
        JasperReport local = compiledReport;
        if (local != null) {
            return local;
        }
        synchronized (ReportService.class) {
            if (compiledReport == null) {
                compiledReport = load();
            }
            return compiledReport;
        }
    }

    /**
     * Charge le modèle précompilé ({@code document.jasper}) ; en son absence, compile le source
     * {@code document.jrxml} à la volée.
     */
    private JasperReport load() {
        InputStream compiled = ReportService.class.getResourceAsStream(COMPILED_PATH);
        if (compiled != null) {
            try (InputStream input = compiled) {
                JasperReport report = (JasperReport) JRLoader.loadObject(input);
                LOG.info("Modèle JasperReports précompilé chargé.");
                return report;
            } catch (JRException ex) {
                LOG.log(Level.WARNING, "Modèle précompilé illisible, compilation du source", ex);
            } catch (java.io.IOException ex) {
                LOG.log(Level.WARNING, "Modèle précompilé illisible, compilation du source", ex);
            }
        }
        InputStream source = ReportService.class.getResourceAsStream(SOURCE_PATH);
        if (source == null) {
            throw new ReportException("Modèle de rapport introuvable dans le WAR : " + SOURCE_PATH);
        }
        try (InputStream input = source) {
            JasperReport report = JasperCompileManager.compileReport(input);
            LOG.info("Modèle JasperReports compilé à la volée depuis " + SOURCE_PATH);
            return report;
        } catch (JRException ex) {
            throw new ReportException("Compilation du modèle JasperReports impossible : "
                    + ex.getMessage(), ex);
        } catch (java.io.IOException ex) {
            throw new ReportException("Lecture du modèle JasperReports impossible", ex);
        }
    }

    private Map<String, Object> buildParameters(Document document, Client client, CompanySettings settings) {
        Map<String, Object> parameters = new HashMap<>();
        String currency = document.getCurrency() == null ? Money.DEFAULT_CURRENCY : document.getCurrency();

        parameters.put("P_COMPANY_NAME", Texts.nz(settings.getCompanyName()));
        parameters.put("P_COMPANY_ADDRESS", Texts.nz(settings.getAddressLine()));
        parameters.put("P_COMPANY_CONTACT", join("   ·   ", settings.getPhone(), settings.getEmail()));
        parameters.put("P_COMPANY_TAX_ID", settings.getTaxId() == null ? "" : "N° fiscal : " + settings.getTaxId());

        parameters.put("P_DOC_TITLE", document.getType().getLabel().toUpperCase(java.util.Locale.FRANCE));
        parameters.put("P_DOC_NUMBER", "N° " + Texts.nz(document.getNumber()));
        parameters.put("P_DOC_DATE", document.getIssueDate() == null
                ? "" : "Émis le " + Dates.format(document.getIssueDate()));
        parameters.put("P_DOC_DUE", document.getDueDate() == null
                ? "" : document.getDueDateLabel() + " " + Dates.format(document.getDueDate()));
        parameters.put("P_SOURCE", document.getSourceDocumentNumber() == null
                ? "" : "Réf. devis " + document.getSourceDocumentNumber());

        parameters.put("P_CLIENT_NAME", Texts.nz(client.getName()));
        parameters.put("P_CLIENT_ADDRESS", Texts.nz(client.getAddressLine()));
        parameters.put("P_CLIENT_CONTACT", join("   ·   ", client.getContactName(), client.getPhone(), client.getEmail()));
        parameters.put("P_CLIENT_TAX_ID", client.getTaxId() == null ? "" : "N° fiscal : " + client.getTaxId());

        parameters.put("P_SUBJECT", Texts.isBlank(document.getSubject()) ? "" : "Objet : " + document.getSubject());

        parameters.put("P_SUBTOTAL", document.getSubtotalText());
        BigDecimal rate = document.getTaxRate() == null ? BigDecimal.ZERO : document.getTaxRate();
        parameters.put("P_TAX_LABEL", "TVA " + rate.stripTrailingZeros().toPlainString() + " %");
        parameters.put("P_TAX", document.getTaxAmountText());
        parameters.put("P_TOTAL", document.getTotalText());
        parameters.put("P_CURRENCY", Money.symbolFor(currency));

        String nature = document.getType().getLabel().toLowerCase(java.util.Locale.FRANCE);
        parameters.put("P_AMOUNT_WORDS",
                "Arrêtée la présente " + nature + " à la somme de : "
                        + AmountToWordsFr.inWords(document.getTotal(), currency));

        parameters.put("P_NOTES", Texts.nz(document.getNotes()));
        parameters.put("P_BANK_DETAILS", Texts.nz(settings.getBankDetails()));
        parameters.put("P_FOOTER_NOTE", Texts.nz(settings.getFooterNote()));
        return parameters;
    }

    private static String join(String separator, String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!Texts.isBlank(value)) {
                if (builder.length() > 0) {
                    builder.append(separator);
                }
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }
}
