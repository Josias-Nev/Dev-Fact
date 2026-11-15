package com.devfact.web;

import com.devfact.model.DocumentLine;

import java.util.ArrayList;
import java.util.List;

/** Aides de préparation des formulaires. */
public final class FormSupport {

    private FormSupport() {
    }

    /** Complète la liste avec des lignes vides pour que le formulaire reste utilisable. */
    public static List<DocumentLine> padLines(List<DocumentLine> lines, int minimum) {
        List<DocumentLine> padded = new ArrayList<>();
        if (lines != null) {
            padded.addAll(lines);
        }
        while (padded.size() < minimum) {
            padded.add(new DocumentLine());
        }
        return padded;
    }
}
