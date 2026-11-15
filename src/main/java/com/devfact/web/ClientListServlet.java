package com.devfact.web;

import com.devfact.model.Client;
import com.devfact.service.ClientService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** Liste des clients. */
@WebServlet(name = "clients", urlPatterns = "/clients")
public class ClientListServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String query = request.getParameter("q");
        boolean showInactive = WebParams.asBoolean(request.getParameter("inactive"));
        List<Client> clients = clientService.search(query, !showInactive);

        request.setAttribute("clients", clients);
        request.setAttribute("q", WebParams.nz(query));
        request.setAttribute("showInactive", Boolean.valueOf(showInactive));
        request.setAttribute("pageTitle", "Clients");
        forward(request, response, "clients.jsp");
    }
}
