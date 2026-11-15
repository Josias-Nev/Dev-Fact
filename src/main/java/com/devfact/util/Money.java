package com.devfact.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Gestion des montants : arrondis par devise et formatage français. */
public final class Money {

    /** Devises sans décimale (le franc CFA en fait partie). */
    private static final Set<String> ZERO_DECIMAL =
            new HashSet<>(Arrays.asList("XOF", "XAF", "XPF", "JPY", "KRW", "CLP", "ISK", "VND", "PYG", "RWF", "UGX", "GNF"));

    public static final String DEFAULT_CURRENCY = "XOF";
    private static final char GROUP_SEPARATOR = '\u00A0'; // espace insécable, présente dans les polices PDF

    private Money() {
    }

    public static boolean isZeroDecimal(String currency) {
        return currency != null && ZERO_DECIMAL.contains(currency.toUpperCase(Locale.ROOT));
    }

    public static int decimalsFor(String currency) {
        return isZeroDecimal(currency) ? 0 : 2;
    }

    public static BigDecimal zero(String currency) {
        return BigDecimal.ZERO.setScale(decimalsFor(currency), RoundingMode.HALF_UP);
    }

    public static BigDecimal round(BigDecimal value, String currency) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(decimalsFor(currency), RoundingMode.HALF_UP);
    }

    public static String symbolFor(String currency) {
        if (currency == null) {
            return "FCFA";
        }
        switch (currency.toUpperCase(Locale.ROOT)) {
            case "XOF":
            case "XAF":
                return "FCFA";
            case "EUR":
                return "€";
            case "USD":
                return "$";
            default:
                return currency;
        }
    }

    /** Exemple : 1 234 567 FCFA */
    public static String format(BigDecimal value, String currency) {
        return formatAmount(value, currency) + " " + symbolFor(currency);
    }

    /** Exemple : 1 234 567 (sans symbole) */
    public static String formatAmount(BigDecimal value, String currency) {
        int decimals = decimalsFor(currency);
        DecimalFormat format = new DecimalFormat();
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        symbols.setGroupingSeparator(GROUP_SEPARATOR);
        symbols.setDecimalSeparator(',');
        format.setDecimalFormatSymbols(symbols);
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);
        format.setGroupingUsed(true);
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(decimals, RoundingMode.HALF_UP);
        return format.format(safe);
    }
}
