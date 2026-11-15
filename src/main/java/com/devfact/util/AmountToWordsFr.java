package com.devfact.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Conversion d'un montant en lettres (français), utilisée sur les documents PDF
 * ("Arrêtée la présente facture à la somme de ...").
 */
public final class AmountToWordsFr {

    private static final String[] UNITS = {
            "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
            "dix-sept", "dix-huit", "dix-neuf"
    };

    private static final String[] TENS = {
            "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante",
            "quatre-vingt", "quatre-vingt"
    };

    private AmountToWordsFr() {
    }

    /** Montant en lettres suivi du nom de la devise : "Trois mille francs CFA". */
    public static String inWords(BigDecimal amount, String currency) {
        String code = currency == null ? Money.DEFAULT_CURRENCY : currency.toUpperCase(Locale.ROOT);
        int decimals = Money.decimalsFor(code);
        BigDecimal value = (amount == null ? BigDecimal.ZERO : amount).setScale(decimals, RoundingMode.HALF_UP);

        long whole = value.longValue();
        BigDecimal fraction = value.subtract(new BigDecimal(whole));
        int cents = fraction.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();

        StringBuilder result = new StringBuilder(capitalize(numberToWords(whole)));
        result.append(' ').append(currencyUnit(code, whole != 1));

        if (decimals == 2 && cents > 0) {
            result.append(" et ").append(numberToWords(cents)).append(" centime");
            if (cents > 1) {
                result.append('s');
            }
        }
        return result.toString();
    }

    /** Nombre entier en lettres (0 à 999 999 999 999). */
    public static String numberToWords(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Montant négatif non supporté : " + value);
        }
        if (value == 0) {
            return UNITS[0];
        }
        if (value >= 1_000_000_000_000L) {
            throw new IllegalArgumentException("Montant trop grand : " + value);
        }

        StringBuilder words = new StringBuilder();
        long billions = value / 1_000_000_000L;
        long millions = (value / 1_000_000L) % 1000L;
        long thousands = (value / 1000L) % 1000L;
        long units = value % 1000L;

        if (billions > 0) {
            append(words, billions == 1 ? "un" : below1000((int) billions, false));
            words.append(" milliard");
            if (billions > 1) {
                words.append('s');
            }
        }
        if (millions > 0) {
            append(words, millions == 1 ? "un" : below1000((int) millions, false));
            words.append(" million");
            if (millions > 1) {
                words.append('s');
            }
        }
        if (thousands > 0) {
            if (thousands > 1) {
                append(words, below1000((int) thousands, false));
            }
            append(words, "mille");
        }
        if (units > 0) {
            append(words, below1000((int) units, true));
        }
        return words.toString();
    }

    private static void append(StringBuilder builder, String chunk) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(chunk);
    }

    private static String below1000(int value, boolean allowPlural) {
        int hundreds = value / 100;
        int remainder = value % 100;
        StringBuilder sb = new StringBuilder();
        switch (hundreds) {
            case 0:
                break;
            case 1:
                sb.append("cent");
                break;
            default:
                sb.append(UNITS[hundreds]).append(" cent");
                if (remainder == 0 && allowPlural) {
                    sb.append('s');
                }
                break;
        }
        if (remainder > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(below100(remainder, allowPlural && hundreds == 0));
        }
        return sb.toString();
    }

    private static String below100(int value, boolean allowPlural) {
        if (value < 20) {
            return UNITS[value];
        }
        int tens = value / 10;
        int unit = value % 10;

        if (value < 70) {
            if (unit == 0) {
                return TENS[tens];
            }
            if (unit == 1) {
                return TENS[tens] + " et un";
            }
            return TENS[tens] + "-" + UNITS[unit];
        }
        if (value < 80) {
            int rest = value - 60;
            if (rest == 11) {
                return "soixante et onze";
            }
            return "soixante-" + below100(rest, allowPlural);
        }
        int rest = value - 80;
        if (rest == 0) {
            return allowPlural ? "quatre-vingts" : "quatre-vingt";
        }
        return "quatre-vingt-" + below100(rest, allowPlural);
    }

    private static String currencyUnit(String currency, boolean plural) {
        switch (currency) {
            case "XOF":
            case "XAF":
            case "XPF":
                return plural ? "francs CFA" : "franc CFA";
            case "EUR":
                return plural ? "euros" : "euro";
            case "USD":
                return plural ? "dollars" : "dollar";
            default:
                return currency;
        }
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
