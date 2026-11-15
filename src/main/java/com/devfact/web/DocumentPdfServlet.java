package com.devfact.web;

import com.devfact.model.Client;
import com.devfact.model.CompanySettings;
import com.devfact.model.Document;
import com.devfact.report.ReportException;
import com.devfact.report.ReportService;
import com.devfact.service.ClientService;
import com.devfact.service.DocumentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Génération du PDF d'un devis ou d'une facture (JasperReports). */
@WebServlet(name = "documentPdf", urlPatterns = "/documents/pdf")
public class DocumentPdfServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final DocumentService documentService = new DocumentService();
    private final ClientService clientService = new ClientService();
    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long id = idParam(request);
        Document document = documentService.load(id);
        if (document == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document introuvable.");
            return;
        }
        Client client = clientService.find(document.getClientId()).orElse(null);
        if (client == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Client introuvable.");
            return;
        }
        CompanySettings settings = documentService.loadSettings();
        try {
            byte[] pdf = reportService.renderPdf(document, document.getLines(), client, settings);
            boolean download = WebParams.asBoolean(request.getParameter("download"));
            response.setContentType("application/pdf");
            response.setContentLength(pdf.length);
            response.setHeader("Content-Disposition",
                    (download ? "attachment" : "inline") + "; filename=\"" + reportService.fileName(document) + "\"");
            response.setHeader("Cache-Control", "no-store");
            response.getOutputStream().write(pdf);
            response.getOutputStream().flush();
        } catch (ReportException ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Génération du PDF impossible : " + ex.getMessage());
        }
    }
}
