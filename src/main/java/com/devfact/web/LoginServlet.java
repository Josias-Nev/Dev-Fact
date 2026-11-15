package com.devfact.web;

import com.devfact.security.AuthService;
import com.devfact.security.AuthenticationException;
import com.devfact.security.UserSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Formulaire de connexion et authentification JAAS. */
@WebServlet(name = "login", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentUser(request) != null) {
            redirect(request, response, "/documents?type=DEVIS");
            return;
        }
        request.setAttribute("next", safeNext(request.getParameter("next")));
        request.setAttribute("databaseAvailable", Boolean.valueOf(AppStatus.isDatabaseAvailable()));
        request.setAttribute("statusMessage", AppStatus.getMessage());
        forward(request, response, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String next = safeNext(request.getParameter("next"));
        try {
            UserSession user = authService.authenticate(username, password);

            HttpSession previous = request.getSession(false);
            if (previous != null) {
                previous.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_USER, user);
            csrfToken(request);
            AppStatus.setDatabaseAvailable(true);

            flashSuccess(request, "Bienvenue " + user.getFullName() + " !");
            redirect(request, response, next == null ? "/documents?type=DEVIS" : next);
        } catch (AuthenticationException ex) {
            request.setAttribute("error", ex.getMessage());
            request.setAttribute("username", WebParams.nz(username));
            request.setAttribute("next", next);
            request.setAttribute("databaseAvailable", Boolean.valueOf(AppStatus.isDatabaseAvailable()));
            request.setAttribute("statusMessage", AppStatus.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            forward(request, response, "login.jsp");
        }
    }

    /** N'accepte que des chemins internes (protection contre les redirections ouvertes). */
    private String safeNext(String next) {
        if (next == null || next.trim().isEmpty()) {
            return null;
        }
        String value = next.trim();
        if (!value.startsWith("/") || value.startsWith("//") || value.contains("://")) {
            return null;
        }
        return value;
    }
}
