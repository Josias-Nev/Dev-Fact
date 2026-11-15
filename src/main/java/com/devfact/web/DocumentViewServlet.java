package com.devfact.web;

import com.devfact.model.Client;
import com.devfact.model.CompanySettings;
import com.devfact.model.Document;
import com.devfact.model.DocumentStatus;
import com.devfact.service.ClientService;
import com.devfact.service.DocumentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Fiche détaillée d'un devis ou d'une facture. */
@WebServlet(name = "documentView", urlPatterns = "/documents/view")
public class DocumentViewServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final DocumentService documentService = new DocumentService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id = idParam(request);
        Document document = documentService.load(id);
        if (document == null) {
            notFound(request, response, "Ce document n'existe pas ou a été supprimé.");
            return;
        }
        Client client = clientService.find(document.getClientId()).orElse(null);
        CompanySettings settings = documentService.loadSettings();

        List<DocumentStatus> nextStatuses = new ArrayList<>();
        for (DocumentStatus candidate : DocumentStatus.forType(document.getType())) {
            if (candidate != DocumentStatus.FACTURE && document.getStatus().canMoveTo(candidate)) {
                nextStatuses.add(candidate);
            }
        }
        boolean invoiced = document.isQuote() && documentService.isInvoiced(document.getId());
        boolean canConvert = document.isQuote() && !invoiced
                && (document.getStatus() == DocumentStatus.ENVOYE || document.getStatus() == DocumentStatus.ACCEPTE);

        request.setAttribute("doc", document);
        request.setAttribute("client", client);
        request.setAttribute("settings", settings);
        request.setAttribute("nextStatuses", nextStatuses);
        request.setAttribute("invoiced", Boolean.valueOf(invoiced));
        request.setAttribute("canConvert", Boolean.valueOf(canConvert));
        request.setAttribute("pageTitle", document.getDisplayTitle());
        forward(request, response, "document-view.jsp");
    }
}
