package com.devfact.web;

import com.devfact.dao.DocumentQuery;
import com.devfact.model.Document;
import com.devfact.model.DocumentStatus;
import com.devfact.model.DocumentType;
import com.devfact.service.ClientService;
import com.devfact.service.DocumentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** Liste des devis ou des factures, avec filtres et pagination. */
@WebServlet(name = "documents", urlPatterns = "/documents")
public class DocumentListServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final DocumentService documentService = new DocumentService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DocumentQuery query = new DocumentQuery();
        query.setType(DocumentType.fromCode(request.getParameter("type")));

        DocumentStatus status = DocumentStatus.fromCode(request.getParameter("status"));
        if (status != null && status.belongsTo(query.getType())) {
            query.setStatus(status);
        }
        query.setClientId(WebParams.asLong(request.getParameter("clientId")));
        query.setText(request.getParameter("q"));
        query.setPage(WebParams.asInt(request.getParameter("page"), 1));

        List<Document> documents = documentService.search(query);
        int total = documentService.count(query);
        int pageCount = (int) Math.ceil(total / (double) query.getPageSize());
        if (pageCount < 1) {
            pageCount = 1;
        }
        if (query.getPage() > pageCount) {
            query.setPage(pageCount);
            documents = documentService.search(query);
        }

        request.setAttribute("documents", documents);
        request.setAttribute("query", query);
        request.setAttribute("total", Integer.valueOf(total));
        request.setAttribute("pageCount", Integer.valueOf(pageCount));
        request.setAttribute("statuses", DocumentStatus.forType(query.getType()));
        request.setAttribute("clients", clientService.findActive());
        request.setAttribute("pageTitle", query.getType().getPluralLabel());
        forward(request, response, "documents.jsp");
    }
}
