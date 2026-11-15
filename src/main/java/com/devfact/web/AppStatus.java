package com.devfact.web;

/** État de santé de l'application, exposé aux pages de connexion. */
public final class AppStatus {

    private static volatile boolean databaseAvailable;
    private static volatile String message = "";

    private AppStatus() {
    }

    public static boolean isDatabaseAvailable() {
        return databaseAvailable;
    }

    public static void setDatabaseAvailable(boolean available) {
        databaseAvailable = available;
    }

    public static String getMessage() {
        return message;
    }

    public static void setMessage(String value) {
        message = value == null ? "" : value;
    }
}
