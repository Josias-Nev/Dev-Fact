package com.devfact.web;

import com.devfact.service.ClientService;
import com.devfact.service.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Suppression, activation ou désactivation d'un client. */
@WebServlet(name = "clientAction", urlPatterns = "/clients/action")
public class ClientActionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ClientService clientService = new ClientService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long id = idParam(request);
        String action = WebParams.nz(request.getParameter("action"));
        try {
            if ("delete".equals(action)) {
                clientService.delete(id);
                flashSuccess(request, "Client supprimé.");
            } else if ("deactivate".equals(action)) {
                clientService.setActive(id, false);
                flashSuccess(request, "Client désactivé.");
            } else if ("activate".equals(action)) {
                clientService.setActive(id, true);
                flashSuccess(request, "Client réactivé.");
            } else {
                throw new ValidationException("Action inconnue : " + action);
            }
        } catch (ValidationException ex) {
            flashError(request, ex.getMessage());
        }
        redirect(request, response, "/clients");
    }
}
