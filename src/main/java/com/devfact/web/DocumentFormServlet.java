package com.devfact.web;

import com.devfact.model.CompanySettings;
import com.devfact.model.Document;
import com.devfact.model.DocumentLine;
import com.devfact.model.DocumentType;
import com.devfact.service.ClientService;
import com.devfact.service.DocumentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Création et modification d'un brouillon de devis ou de facture. */
@WebServlet(name = "documentForm", urlPatterns = {"/documents/new", "/documents/edit"})
public class DocumentFormServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final DocumentService documentService = new DocumentService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CompanySettings settings = documentService.loadSettings();
        String path = request.getServletPath();
        Document document;
        boolean isNew;
        List<DocumentLine> lines;

        if (path != null && path.endsWith("/edit")) {
            long id = idParam(request);
            document = documentService.load(id);
            if (document == null) {
                notFound(request, response, "Ce document n'existe pas ou a été supprimé.");
                return;
            }
            if (!document.isEditable()) {
                flashError(request, "Seul un document au statut « Brouillon » peut être modifié.");
                redirect(request, response, "/documents/view?id=" + id);
                return;
            }
            lines = document.getLines();
            isNew = false;
        } else {
            DocumentType type = DocumentType.fromCode(request.getParameter("type"));
            document = new Document();
            document.setType(type);
            document.setIssueDate(LocalDate.now());
            document.setTaxRate(settings.getDefaultTaxRate());
            document.setCurrency(settings.getCurrency());
            int days = type == DocumentType.DEVIS ? settings.getQuoteValidityDays() : settings.getPaymentTermsDays();
            document.setDueDate(LocalDate.now().plusDays(Math.max(0, days)));
            lines = new ArrayList<>();
            isNew = true;
        }

        request.setAttribute("doc", document);
        request.setAttribute("lines", FormSupport.padLines(lines, 3));
        request.setAttribute("clients", clientService.findActive());
        request.setAttribute("settings", settings);
        request.setAttribute("isNew", Boolean.valueOf(isNew));
        request.setAttribute("pageTitle", (isNew ? "Nouveau " : "Modifier le ")
                + document.getType().getLabel().toLowerCase(java.util.Locale.FRANCE));
        forward(request, response, "document-form.jsp");
    }
}
