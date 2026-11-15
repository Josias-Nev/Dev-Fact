package com.devfact.service;

import com.devfact.dao.ClientDao;
import com.devfact.dao.DocumentDao;
import com.devfact.dao.DocumentQuery;
import com.devfact.dao.SequenceDao;
import com.devfact.dao.SettingsDao;
import com.devfact.db.DataAccess;
import com.devfact.model.Client;
import com.devfact.model.CompanySettings;
import com.devfact.model.Document;
import com.devfact.model.DocumentLine;
import com.devfact.model.DocumentStatus;
import com.devfact.model.DocumentType;
import com.devfact.util.Money;
import com.devfact.util.Texts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Logique métier des devis et factures : numérotation, totaux, cycles de vie, conversion. */
public class DocumentService {

    public static final int MAX_LINES = 60;

    private final DocumentDao documentDao = new DocumentDao();
    private final ClientDao clientDao = new ClientDao();
    private final SettingsDao settingsDao = new SettingsDao();
    private final SequenceDao sequenceDao = new SequenceDao();

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    public Document load(long id) {
        return DataAccess.read(connection -> {
            Document document = documentDao.findById(connection, id).orElse(null);
            if (document != null) {
                document.setLines(documentDao.findLines(connection, id));
            }
            return document;
        });
    }

    public CompanySettings loadSettings() {
        return DataAccess.read(connection -> settingsDao.load(connection));
    }

    public List<Document> search(DocumentQuery query) {
        return DataAccess.read(connection -> documentDao.search(connection, query));
    }

    public int count(DocumentQuery query) {
        return DataAccess.read(connection -> documentDao.count(connection, query));
    }

    // ------------------------------------------------------------------
    // Écriture
    // ------------------------------------------------------------------

    /** Crée un document en brouillon et lui attribue son numéro définitif. */
    public Document create(Document document, List<DocumentLine> lines, Long userId) {
        List<DocumentLine> cleaned = cleanLines(lines);
        CompanySettings settings = loadSettings();
        String currency = Texts.isBlank(document.getCurrency()) ? settings.getCurrency() : document.getCurrency();
        applyDefaults(document, settings);
        validate(document, cleaned);

        final Totals totals = computeTotals(cleaned, document.getTaxRate(), currency);
        document.setCurrency(currency);
        document.setSubtotal(totals.getSubtotal());
        document.setTaxAmount(totals.getTaxAmount());
        document.setTotal(totals.getTotal());
        document.setStatus(DocumentStatus.BROUILLON);
        document.setCreatedBy(userId);
        document.setLines(cleaned);

        DataAccess.write(connection -> {
            requireActiveClient(connection, document.getClientId());
            document.setNumber(nextNumber(connection, document.getType(), document.getIssueDate().getYear(), settings));
            long id = documentDao.insert(connection, document);
            documentDao.insertLines(connection, id, cleaned);
            return id;
        });
        return document;
    }

    /** Met à jour un brouillon (en-tête et lignes). */
    public Document update(Document document, List<DocumentLine> lines) {
        if (document.getId() == null) {
            throw new ValidationException("Document introuvable.");
        }
        Document existing = load(document.getId());
        if (existing == null) {
            throw new ValidationException("Document introuvable.");
        }
        if (!existing.isEditable()) {
            throw new ValidationException("Seul un document au statut « Brouillon » peut être modifié.");
        }
        List<DocumentLine> cleaned = cleanLines(lines);
        CompanySettings settings = loadSettings();
        String currency = Texts.isBlank(existing.getCurrency()) ? settings.getCurrency() : existing.getCurrency();
        applyDefaults(document, settings);
        validate(document, cleaned);

        Totals totals = computeTotals(cleaned, document.getTaxRate(), currency);
        document.setType(existing.getType());
        document.setNumber(existing.getNumber());
        document.setStatus(existing.getStatus());
        document.setCurrency(currency);
        document.setSubtotal(totals.getSubtotal());
        document.setTaxAmount(totals.getTaxAmount());
        document.setTotal(totals.getTotal());
        document.setLines(cleaned);

        DataAccess.write(connection -> {
            requireActiveClient(connection, document.getClientId());
            documentDao.updateHeader(connection, document);
            documentDao.deleteLines(connection, document.getId());
            documentDao.insertLines(connection, document.getId(), cleaned);
            return null;
        });
        return document;
    }

    /** Change le statut d'un document en respectant les transitions autorisées. */
    public void changeStatus(long id, DocumentStatus target) {
        if (target == null) {
            throw new ValidationException("Statut cible inconnu.");
        }
        DataAccess.write(connection -> {
            Document document = documentDao.findById(connection, id)
                    .orElseThrow(() -> new ValidationException("Document introuvable."));
            if (!target.belongsTo(document.getType())) {
                throw new ValidationException("Le statut « " + target.getLabel()
                        + " » ne s'applique pas à un " + document.getType().getLabel().toLowerCase() + ".");
            }
            if (target == DocumentStatus.FACTURE) {
                throw new ValidationException("Utilisez l'action « Convertir en facture » pour facturer un devis.");
            }
            if (!document.getStatus().canMoveTo(target)) {
                throw new ValidationException("Passage de « " + document.getStatus().getLabel()
                        + " » à « " + target.getLabel() + " » non autorisé.");
            }
            documentDao.updateStatus(connection, id, target);
            return null;
        });
    }

    /**
     * Convertit un devis accepté (ou envoyé) en facture brouillon, en recopiant les lignes.
     *
     * @return identifiant de la facture créée
     */
    public long convertToInvoice(long quoteId, Long userId) {
        CompanySettings settings = loadSettings();
        return DataAccess.write(connection -> {
            Document quote = documentDao.findById(connection, quoteId)
                    .orElseThrow(() -> new ValidationException("Devis introuvable."));
            if (quote.getType() != DocumentType.DEVIS) {
                throw new ValidationException("Seul un devis peut être converti en facture.");
            }
            if (documentDao.findInvoiceIdForQuote(connection, quoteId).isPresent()) {
                throw new ValidationException("Ce devis a déjà été converti en facture.");
            }
            if (quote.getStatus() != DocumentStatus.ACCEPTE && quote.getStatus() != DocumentStatus.ENVOYE) {
                throw new ValidationException("Seul un devis « Envoyé » ou « Accepté » peut être facturé.");
            }
            List<DocumentLine> lines = documentDao.findLines(connection, quoteId);
            if (lines.isEmpty()) {
                throw new ValidationException("Le devis ne contient aucune ligne.");
            }

            Document invoice = new Document();
            invoice.setType(DocumentType.FACTURE);
            invoice.setClientId(quote.getClientId());
            invoice.setStatus(DocumentStatus.BROUILLON);
            invoice.setIssueDate(LocalDate.now());
            invoice.setDueDate(LocalDate.now().plusDays(Math.max(0, settings.getPaymentTermsDays())));
            invoice.setSubject(quote.getSubject());
            invoice.setNotes(quote.getNotes());
            invoice.setTaxRate(quote.getTaxRate() != null ? quote.getTaxRate() : settings.getDefaultTaxRate());
            invoice.setSubtotal(quote.getSubtotal());
            invoice.setTaxAmount(quote.getTaxAmount());
            invoice.setTotal(quote.getTotal());
            invoice.setCurrency(quote.getCurrency());
            invoice.setSourceDocumentId(quote.getId());
            invoice.setCreatedBy(userId);
            invoice.setNumber(nextNumber(connection, DocumentType.FACTURE, invoice.getIssueDate().getYear(), settings));

            long invoiceId = documentDao.insert(connection, invoice);
            List<DocumentLine> copied = new ArrayList<>();
            for (DocumentLine line : lines) {
                copied.add(new DocumentLine(line.getDescription(), line.getQuantity(), line.getUnitPrice()));
            }
            computeTotals(copied, invoice.getTaxRate(), invoice.getCurrency());
            documentDao.insertLines(connection, invoiceId, copied);
            documentDao.updateStatus(connection, quoteId, DocumentStatus.FACTURE);
            return invoiceId;
        });
    }

    /** Indique si un devis a déjà été converti en facture. */
    public boolean isInvoiced(long quoteId) {
        return DataAccess.read(connection -> documentDao.findInvoiceIdForQuote(connection, quoteId).isPresent());
    }

    /** Supprime un brouillon. */
    public void delete(long id) {
        DataAccess.write(connection -> {
            Document document = documentDao.findById(connection, id)
                    .orElseThrow(() -> new ValidationException("Document introuvable."));
            if (!document.isEditable()) {
                throw new ValidationException("Seul un document au statut « Brouillon » peut être supprimé.");
            }
            documentDao.delete(connection, id);
            return null;
        });
    }

    // ------------------------------------------------------------------
    // Calculs et validation
    // ------------------------------------------------------------------

    /** Calcule le total de chaque ligne puis les totaux du document. */
    public Totals computeTotals(List<DocumentLine> lines, BigDecimal taxRate, String currency) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DocumentLine line : lines) {
            BigDecimal quantity = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
            BigDecimal unitPrice = line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice();
            BigDecimal lineTotal = Money.round(quantity.multiply(unitPrice), currency);
            line.setLineTotal(lineTotal);
            line.setCurrency(currency);
            subtotal = subtotal.add(lineTotal);
        }
        BigDecimal rate = taxRate == null ? BigDecimal.ZERO : taxRate;
        BigDecimal taxAmount = Money.round(
                subtotal.multiply(rate).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP), currency);
        BigDecimal total = subtotal.add(taxAmount);
        return new Totals(subtotal, taxAmount, total);
    }

    /** Retire les lignes vides et normalise les valeurs par défaut. */
    public List<DocumentLine> cleanLines(List<DocumentLine> lines) {
        List<DocumentLine> cleaned = new ArrayList<>();
        if (lines == null) {
            return cleaned;
        }
        for (DocumentLine line : lines) {
            if (line == null) {
                continue;
            }
            String description = Texts.trimToNull(line.getDescription());
            BigDecimal quantity = line.getQuantity();
            BigDecimal unitPrice = line.getUnitPrice();
            boolean empty = description == null
                    && (quantity == null || quantity.signum() == 0)
                    && (unitPrice == null || unitPrice.signum() == 0);
            if (empty) {
                continue;
            }
            DocumentLine copy = new DocumentLine();
            copy.setDescription(description);
            copy.setQuantity(quantity == null ? BigDecimal.ONE : quantity);
            copy.setUnitPrice(unitPrice == null ? BigDecimal.ZERO : unitPrice);
            copy.setCurrency(line.getCurrency());
            cleaned.add(copy);
        }
        return cleaned;
    }

    /** Valide l'en-tête et les lignes ; lève une exception listant tous les problèmes. */
    public void validate(Document document, List<DocumentLine> lines) {
        List<String> errors = new ArrayList<>();
        document.setSubject(Texts.trimToNull(document.getSubject()));
        document.setNotes(Texts.trimToNull(document.getNotes()));

        if (document.getClientId() == null) {
            errors.add("Veuillez sélectionner un client.");
        }
        if (document.getIssueDate() == null) {
            errors.add("La date d'émission est obligatoire.");
        }
        if (document.getDueDate() != null && document.getIssueDate() != null
                && document.getDueDate().isBefore(document.getIssueDate())) {
            errors.add("La date d'échéance ne peut pas précéder la date d'émission.");
        }
        if (document.getSubject() != null && document.getSubject().length() > 255) {
            errors.add("L'objet ne peut pas dépasser 255 caractères.");
        }
        if (document.getTaxRate() == null) {
            errors.add("Le taux de TVA est obligatoire (0 si non applicable).");
        } else if (document.getTaxRate().signum() < 0
                || document.getTaxRate().compareTo(new BigDecimal("100")) > 0) {
            errors.add("Le taux de TVA doit être comprise entre 0 et 100 %.");
        }
        if (lines.isEmpty()) {
            errors.add("Ajoutez au moins une ligne au document.");
        }
        if (lines.size() > MAX_LINES) {
            errors.add("Un document ne peut pas contenir plus de " + MAX_LINES + " lignes.");
        }
        int index = 1;
        for (DocumentLine line : lines) {
            String prefix = "Ligne " + index + " : ";
            if (Texts.isBlank(line.getDescription())) {
                errors.add(prefix + "la désignation est obligatoire.");
            } else if (line.getDescription().length() > 255) {
                errors.add(prefix + "la désignation ne peut pas dépasser 255 caractères.");
            }
            if (line.getQuantity() == null || line.getQuantity().signum() <= 0) {
                errors.add(prefix + "la quantité doit être supérieure à zéro.");
            }
            if (line.getUnitPrice() == null || line.getUnitPrice().signum() < 0) {
                errors.add(prefix + "le prix unitaire doit être positif ou nul.");
            }
            index++;
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    // ------------------------------------------------------------------
    // Helpers internes
    // ------------------------------------------------------------------

    private void applyDefaults(Document document, CompanySettings settings) {
        if (document.getIssueDate() == null) {
            document.setIssueDate(LocalDate.now());
        }
        if (document.getTaxRate() == null) {
            document.setTaxRate(settings.getDefaultTaxRate());
        }
        if (document.getDueDate() == null) {
            int days = document.isQuote() ? settings.getQuoteValidityDays() : settings.getPaymentTermsDays();
            document.setDueDate(document.getIssueDate().plusDays(Math.max(0, days)));
        }
        if (document.getStatus() == null) {
            document.setStatus(DocumentStatus.BROUILLON);
        }
    }

    private void requireActiveClient(Connection connection, Long clientId) throws SQLException {
        Client client = clientDao.findById(connection, clientId).orElse(null);
        if (client == null) {
            throw new ValidationException("Le client sélectionné n'existe pas.");
        }
        if (!client.isActive()) {
            throw new ValidationException("Le client « " + client.getName() + " » est désactivé.");
        }
    }

    private String nextNumber(Connection connection, DocumentType type, int year, CompanySettings settings)
            throws SQLException {
        int value = sequenceDao.nextValue(connection, type.name(), year);
        String prefix = type == DocumentType.DEVIS ? settings.getQuotePrefix() : settings.getInvoicePrefix();
        if (Texts.isBlank(prefix)) {
            prefix = type.getDefaultPrefix();
        }
        return prefix + "-" + year + "-" + String.format("%04d", value);
    }
}
