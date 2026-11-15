package com.devfact.security;

import com.devfact.dao.UserDao;
import com.devfact.db.DataAccess;
import com.devfact.model.Role;
import com.devfact.model.UserAccount;

/**
 * Outil en ligne de commande pour la gestion des comptes.
 * <pre>
 *   # Générer une empreinte de mot de passe
 *   java -cp dev-fact.war com.devfact.security.PasswordTool "MonMotDePasse"
 *
 *   # Créer un utilisateur directement en base
 *   java -cp dev-fact.war com.devfact.security.PasswordTool --create \
 *        jdupont "MonMotDePasse" ADMIN "Jean Dupont" jdupont@example.com
 * </pre>
 * Les paramètres de connexion suivent la configuration de l'application
 * (variables d'environnement DEV_FACT_DB_URL / DEV_FACT_DB_USER / DEV_FACT_DB_PASSWORD).
 */
public final class PasswordTool {

    private PasswordTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }
        if ("--create".equals(args[0])) {
            if (args.length < 4) {
                printUsage();
                System.exit(2);
            }
            String username = args[1];
            String password = args[2];
            Role role = Role.fromCode(args[3]);
            String fullName = args.length > 4 ? args[4] : username;
            String email = args.length > 5 ? args[5] : null;

            UserAccount account = new UserAccount();
            account.setUsername(username);
            account.setPasswordHash(PasswordHasher.hash(password));
            account.setRole(role);
            account.setFullName(fullName);
            account.setEmail(email);
            account.setEnabled(true);

            Long id = DataAccess.write(connection -> new UserDao().insert(connection, account));
            System.out.println("Utilisateur créé : " + username + " (id=" + id + ", rôle=" + role + ")");
            return;
        }
        System.out.println(PasswordHasher.hash(args[0]));
    }

    private static void printUsage() {
        System.out.println("Usage :");
        System.out.println("  PasswordTool <motDePasse>");
        System.out.println("  PasswordTool --create <login> <motDePasse> <ADMIN|COMMERCIAL> [nomComplet] [email]");
    }
}
