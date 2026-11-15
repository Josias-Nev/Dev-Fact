package com.devfact.web;

import java.io.Serializable;

/** Message flash affiché une seule fois sur la page suivante. */
public class FlashMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String INFO = "info";

    private final String type;
    private final String message;

    public FlashMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return SUCCESS.equals(type);
    }

    public boolean isError() {
        return ERROR.equals(type);
    }
}
