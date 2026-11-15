package com.devfact.web;

import com.devfact.model.Client;
import com.devfact.service.ClientService;
import com.devfact.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Enregistrement d'un client. */
@WebServlet(name = "clientSave", urlPatterns = "/clients/save")
public class ClientSaveServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ClientService clientService = new ClientService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Client client = new Client();
        client.setId(WebParams.asLong(request.getParameter("id")));
        client.setName(request.getParameter("name"));
        client.setContactName(request.getParameter("contactName"));
        client.setEmail(request.getParameter("email"));
        client.setPhone(request.getParameter("phone"));
        client.setAddress(request.getParameter("address"));
        client.setCity(request.getParameter("city"));
        client.setTaxId(request.getParameter("taxId"));
        client.setNotes(request.getParameter("notes"));
        client.setActive(isChecked(request.getParameterValues("active")));

        try {
            clientService.save(client);
            flashSuccess(request, client.getId() == null
                    ? "Client « " + client.getName() + " » créé."
                    : "Client « " + client.getName() + " » mis à jour.");
            redirect(request, response, "/clients");
        } catch (ValidationException ex) {
            request.setAttribute("errors", ex.getErrors());
            request.setAttribute("client", client);
            request.setAttribute("isNew", Boolean.valueOf(client.getId() == null));
            request.setAttribute("pageTitle", client.getId() == null ? "Nouveau client" : "Modifier le client");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            forward(request, response, "client-form.jsp");
        }
    }

    /** Une case à cocher est active si la valeur "true" fait partie des valeurs envoyées. */
    private boolean isChecked(String[] values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
