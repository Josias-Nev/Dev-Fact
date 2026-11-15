package com.devfact.web;

import com.devfact.util.Dates;
import com.devfact.util.Texts;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lecture tolérante des paramètres de formulaire. */
public final class WebParams {

    private WebParams() {
    }

    public static String text(jakarta.servlet.http.HttpServletRequest request, String name) {
        return Texts.trimToNull(request.getParameter(name));
    }

    public static Long asLong(String raw) {
        if (Texts.isBlank(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static long asLong(String raw, long fallback) {
        Long value = asLong(raw);
        return value == null ? fallback : value.longValue();
    }

    public static int asInt(String raw, int fallback) {
        if (Texts.isBlank(raw)) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static boolean asBoolean(String raw) {
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "on".equalsIgnoreCase(raw);
    }

    public static LocalDate asDate(String raw) {
        return Dates.parse(raw);
    }

    /**
     * Convertit une saisie décimale française ou anglaise : "1 234,56", "1234.56", "18 %".
     *
     * @return null si la valeur est vide ou non numérique
     */
    public static BigDecimal asDecimal(String raw) {
        if (Texts.isBlank(raw)) {
            return null;
        }
        String cleaned = raw.replace('\u00A0', ' ').replace(" ", "").replace("%", "").trim();
        if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned = cleaned.replace(".", "").replace(',', '.');
        } else {
            cleaned = cleaned.replace(',', '.');
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String nz(String value) {
        return value == null ? "" : value;
    }
}
