package com.devfact.web;

import com.devfact.model.Client;
import com.devfact.service.ClientService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Création / modification d'une fiche client. */
@WebServlet(name = "clientForm", urlPatterns = {"/clients/new", "/clients/edit"})
public class ClientFormServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        Client client;
        boolean isNew;
        if (path != null && path.endsWith("/edit")) {
            long id = idParam(request);
            client = clientService.find(id).orElse(null);
            if (client == null) {
                notFound(request, response, "Ce client n'existe pas.");
                return;
            }
            isNew = false;
        } else {
            client = new Client();
            client.setActive(true);
            isNew = true;
        }
        request.setAttribute("client", client);
        request.setAttribute("isNew", Boolean.valueOf(isNew));
        request.setAttribute("pageTitle", isNew ? "Nouveau client" : "Modifier le client");
        forward(request, response, "client-form.jsp");
    }
}
