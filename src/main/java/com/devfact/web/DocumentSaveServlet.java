package com.devfact.web;

import com.devfact.model.Document;
import com.devfact.model.DocumentLine;
import com.devfact.model.DocumentType;
import com.devfact.security.UserSession;
import com.devfact.service.ClientService;
import com.devfact.service.DocumentService;
import com.devfact.service.ValidationException;
import com.devfact.util.Texts;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Enregistrement (création ou mise à jour) d'un devis / d'une facture. */
@WebServlet(name = "documentSave", urlPatterns = "/documents/save")
public class DocumentSaveServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final DocumentService documentService = new DocumentService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserSession user = currentUser(request);
        List<String> errors = new ArrayList<>();
        Document document = bindDocument(request, errors);
        List<DocumentLine> lines = bindLines(request, errors);

        try {
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
            Document saved;
            if (document.getId() == null) {
                saved = documentService.create(document, lines, user == null ? null : user.getUserId());
                flashSuccess(request, saved.getType().getLabel() + " " + saved.getNumber() + " créé.");
            } else {
                saved = documentService.update(document, lines);
                flashSuccess(request, saved.getType().getLabel() + " " + saved.getNumber() + " mis à jour.");
            }
            redirect(request, response, "/documents/view?id=" + saved.getId());
        } catch (ValidationException ex) {
            request.setAttribute("errors", ex.getErrors());
            request.setAttribute("doc", document);
            request.setAttribute("lines", FormSupport.padLines(lines, 3));
            request.setAttribute("clients", clientService.findActive());
            request.setAttribute("settings", documentService.loadSettings());
            request.setAttribute("isNew", Boolean.valueOf(document.getId() == null));
            request.setAttribute("pageTitle", document.getId() == null
                    ? "Nouveau " + document.getType().getLabel().toLowerCase(java.util.Locale.FRANCE)
                    : "Modifier le " + document.getType().getLabel().toLowerCase(java.util.Locale.FRANCE));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            forward(request, response, "document-form.jsp");
        }
    }

    private Document bindDocument(HttpServletRequest request, List<String> errors) {
        Document document = new Document();
        document.setId(WebParams.asLong(request.getParameter("id")));
        document.setType(DocumentType.fromCode(request.getParameter("type")));
        document.setClientId(WebParams.asLong(request.getParameter("clientId")));
        document.setSubject(request.getParameter("subject"));
        document.setNotes(request.getParameter("notes"));

        LocalDate issueDate = WebParams.asDate(request.getParameter("issueDate"));
        document.setIssueDate(issueDate == null ? LocalDate.now() : issueDate);
        document.setDueDate(WebParams.asDate(request.getParameter("dueDate")));

        String rawTax = request.getParameter("taxRate");
        BigDecimal taxRate = WebParams.asDecimal(rawTax);
        if (taxRate == null && !Texts.isBlank(rawTax)) {
            errors.add("Le taux de TVA saisi n'est pas un nombre valide.");
        }
        document.setTaxRate(taxRate);
        return document;
    }

    private List<DocumentLine> bindLines(HttpServletRequest request, List<String> errors) {
        String[] descriptions = request.getParameterValues("lineDescription");
        String[] quantities = request.getParameterValues("lineQuantity");
        String[] prices = request.getParameterValues("lineUnitPrice");
        List<DocumentLine> lines = new ArrayList<>();
        if (descriptions == null) {
            return lines;
        }
        for (int i = 0; i < descriptions.length; i++) {
            String rawDescription = descriptions[i];
            String rawQuantity = valueAt(quantities, i);
            String rawPrice = valueAt(prices, i);

            DocumentLine line = new DocumentLine();
            line.setDescription(rawDescription);
            if (Texts.isBlank(rawDescription) && Texts.isBlank(rawQuantity) && Texts.isBlank(rawPrice)) {
                continue; // ligne entièrement vide
            }
            BigDecimal quantity = WebParams.asDecimal(rawQuantity);
            if (!Texts.isBlank(rawQuantity) && quantity == null) {
                errors.add("Ligne " + (i + 1) + " : la quantité n'est pas un nombre valide.");
            }
            BigDecimal price = WebParams.asDecimal(rawPrice);
            if (!Texts.isBlank(rawPrice) && price == null) {
                errors.add("Ligne " + (i + 1) + " : le prix unitaire n'est pas un nombre valide.");
            }
            line.setQuantity(quantity == null ? BigDecimal.ONE : quantity);
            line.setUnitPrice(price == null ? BigDecimal.ZERO : price);
            lines.add(line);
        }
        return lines;
    }

    private static String valueAt(String[] values, int index) {
        return values != null && index < values.length ? values[index] : null;
    }
}
