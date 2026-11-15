package com.devfact.web;

import com.devfact.model.CompanySettings;
import com.devfact.service.SettingsService;
import com.devfact.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** Paramètres de la société (réservé aux administrateurs). */
@WebServlet(name = "settings", urlPatterns = "/settings")
public class SettingsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final SettingsService settingsService = new SettingsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        request.setAttribute("settings", settingsService.load());
        request.setAttribute("pageTitle", "Paramètres");
        forward(request, response, "settings.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        CompanySettings settings = new CompanySettings();
        settings.setCompanyName(request.getParameter("companyName"));
        settings.setAddress(request.getParameter("address"));
        settings.setCity(request.getParameter("city"));
        settings.setPhone(request.getParameter("phone"));
        settings.setEmail(request.getParameter("email"));
        settings.setTaxId(request.getParameter("taxId"));
        settings.setBankDetails(request.getParameter("bankDetails"));
        settings.setFooterNote(request.getParameter("footerNote"));
        settings.setCurrency(request.getParameter("currency"));
        settings.setQuotePrefix(request.getParameter("quotePrefix"));
        settings.setInvoicePrefix(request.getParameter("invoicePrefix"));
        settings.setPaymentTermsDays(WebParams.asInt(request.getParameter("paymentTermsDays"), 30));
        settings.setQuoteValidityDays(WebParams.asInt(request.getParameter("quoteValidityDays"), 30));

        BigDecimal taxRate = WebParams.asDecimal(request.getParameter("defaultTaxRate"));
        settings.setDefaultTaxRate(taxRate == null ? new BigDecimal("18.00") : taxRate);

        try {
            settingsService.save(settings);
            flashSuccess(request, "Paramètres enregistrés.");
            redirect(request, response, "/settings");
        } catch (ValidationException ex) {
            request.setAttribute("errors", ex.getErrors());
            request.setAttribute("settings", settings);
            request.setAttribute("pageTitle", "Paramètres");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            forward(request, response, "settings.jsp");
        }
    }
}
