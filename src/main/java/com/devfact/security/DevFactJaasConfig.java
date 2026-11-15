package com.devfact.security;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration JAAS déclarative (équivalent d'un fichier jaas.conf) qui enregistre
 * {@link DevFactLoginModule} pour le contexte de connexion « DevFact ».
 */
public class DevFactJaasConfig extends Configuration {

    public static final String CONTEXT_NAME = "DevFact";

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (!CONTEXT_NAME.equals(name)) {
            return null;
        }
        Map<String, String> options = new HashMap<>();
        options.put("debug", "false");
        AppConfigurationEntry entry = new AppConfigurationEntry(
                DevFactLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                options);
        return new AppConfigurationEntry[]{entry};
    }
}
