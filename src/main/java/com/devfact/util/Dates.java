package com.devfact.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Formatage et lecture des dates (format français pour l'affichage, ISO pour les formulaires). */
public final class Dates {

    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Dates() {
    }

    public static LocalDate parse(String value) {
        if (Texts.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value.trim(), DISPLAY);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static String format(LocalDate date) {
        return date == null ? "" : DISPLAY.format(date);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : DISPLAY_TIME.format(dateTime);
    }

    public static String iso(LocalDate date) {
        return date == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    public static String iso(LocalDateTime dateTime) {
        return dateTime == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
    }
}
