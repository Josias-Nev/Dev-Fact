package com.devfact.web;

import com.devfact.model.DocumentStatus;
import com.devfact.model.DocumentType;
import com.devfact.security.UserSession;
import com.devfact.service.DocumentService;
import com.devfact.service.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Actions sur un document : changement de statut, conversion en facture, suppression. */
@WebServlet(name = "documentAction", urlPatterns = "/documents/action")
public class DocumentActionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final DocumentService documentService = new DocumentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long id = idParam(request);
        String action = WebParams.nz(request.getParameter("action"));
        UserSession user = currentUser(request);

        try {
            if ("status".equals(action)) {
                DocumentStatus target = DocumentStatus.fromCode(request.getParameter("status"));
                documentService.changeStatus(id, target);
                flashSuccess(request, "Statut mis à jour : "
                        + (target == null ? "" : target.getLabel()) + ".");
            } else if ("convert".equals(action)) {
                long invoiceId = documentService.convertToInvoice(id, user == null ? null : user.getUserId());
                flashSuccess(request, "Le devis a été converti en facture (brouillon).");
                redirect(request, response, "/documents/view?id=" + invoiceId);
                return;
            } else if ("delete".equals(action)) {
                documentService.delete(id);
                flashSuccess(request, "Brouillon supprimé.");
                DocumentType type = DocumentType.fromCode(request.getParameter("type"));
                redirect(request, response, "/documents?type=" + type.getCode());
                return;
            } else {
                throw new ValidationException("Action inconnue : " + action);
            }
        } catch (ValidationException ex) {
            flashError(request, ex.getMessage());
        }
        redirect(request, response, "/documents/view?id=" + id);
    }
}
