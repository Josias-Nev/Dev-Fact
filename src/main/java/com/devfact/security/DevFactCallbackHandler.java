package com.devfact.security;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/** Transmet les identifiants saisis dans le formulaire au module de connexion JAAS. */
public class DevFactCallbackHandler implements CallbackHandler {

    private final String username;
    private final char[] password;

    public DevFactCallbackHandler(String username, String password) {
        this.username = username;
        this.password = password == null ? new char[0] : password.toCharArray();
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                ((NameCallback) callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
                ((PasswordCallback) callback).setPassword(password);
            } else {
                throw new UnsupportedCallbackException(callback, "Callback non supporté : " + callback.getClass());
            }
        }
    }
}
