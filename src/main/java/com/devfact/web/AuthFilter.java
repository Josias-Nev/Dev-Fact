package com.devfact.web;

import com.devfact.security.UserSession;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Contrôle d'accès : toute page nécessite une session JAAS valide.
 * Les requêtes POST doivent également porter un jeton CSRF valide.
 */
@WebFilter(urlPatterns = "/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isPublic(path)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpSession session = request.getSession(false);
        UserSession user = session == null ? null : (UserSession) session.getAttribute(BaseServlet.SESSION_USER);
        if (user == null) {
            String target = request.getRequestURI().substring(request.getContextPath().length());
            String query = request.getQueryString();
            if (query != null && !query.isEmpty()) {
                target = target + "?" + query;
            }
            response.sendRedirect(request.getContextPath() + "/login?next="
                    + URLEncoder.encode(target, "UTF-8"));
            return;
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) && !validCsrf(request, session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Jeton de sécurité invalide ou expiré. Rechargez la page.");
            return;
        }

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        chain.doFilter(servletRequest, servletResponse);
    }

    private boolean isPublic(String path) {
        return path.isEmpty()
                || "/".equals(path)
                || "/login".equals(path)
                || "/index.jsp".equals(path)
                || "/favicon.ico".equals(path)
                || path.startsWith("/assets/");
    }

    private boolean validCsrf(HttpServletRequest request, HttpSession session) {
        Object expected = session.getAttribute(BaseServlet.SESSION_CSRF);
        String provided = request.getParameter("_csrf");
        return expected instanceof String && provided != null && expected.equals(provided);
    }
}
