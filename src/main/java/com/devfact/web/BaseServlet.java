package com.devfact.web;

import com.devfact.security.UserSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Base commune des servlets : session, jetons CSRF, messages flash, redirections. */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_USER = "user";
    public static final String SESSION_CSRF = "csrfToken";
    private static final String SESSION_FLASH = "flash";
    private static final SecureRandom RANDOM = new SecureRandom();

    protected void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.setAttribute("flash", consumeFlash(request));
        request.setAttribute("currentUser", currentUser(request));
        request.setAttribute("csrfToken", csrfToken(request));
        request.getRequestDispatcher("/WEB-INF/views/" + view).forward(request, response);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    protected UserSession currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (UserSession) session.getAttribute(SESSION_USER);
    }

    protected String csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SESSION_CSRF);
        if (existing instanceof String) {
            return (String) existing;
        }
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_CSRF, token);
        return token;
    }

    @SuppressWarnings("unchecked")
    protected void flash(HttpServletRequest request, String type, String message) {
        HttpSession session = request.getSession(true);
        List<FlashMessage> messages = (List<FlashMessage>) session.getAttribute(SESSION_FLASH);
        if (messages == null) {
            messages = new ArrayList<>();
            session.setAttribute(SESSION_FLASH, messages);
        }
        messages.add(new FlashMessage(type, message));
    }

    protected void flashSuccess(HttpServletRequest request, String message) {
        flash(request, FlashMessage.SUCCESS, message);
    }

    protected void flashError(HttpServletRequest request, String message) {
        flash(request, FlashMessage.ERROR, message);
    }

    @SuppressWarnings("unchecked")
    private List<FlashMessage> consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return new ArrayList<>();
        }
        List<FlashMessage> messages = (List<FlashMessage>) session.getAttribute(SESSION_FLASH);
        session.removeAttribute(SESSION_FLASH);
        return messages == null ? new ArrayList<FlashMessage>() : messages;
    }

    protected boolean requireAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        UserSession user = currentUser(request);
        if (user == null || !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès réservé aux administrateurs.");
            return false;
        }
        return true;
    }

    protected void notFound(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("errorTitle", "Élément introuvable");
        request.setAttribute("errorMessage", message);
        forward(request, response, "error.jsp");
    }

    protected long idParam(HttpServletRequest request) {
        return WebParams.asLong(request.getParameter("id"), -1L);
    }

    protected String ctx(HttpServletRequest request) {
        return request.getContextPath();
    }
}
