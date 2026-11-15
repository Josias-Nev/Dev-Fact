-- ============================================================================
--  Dev-Fact - Données initiales
--  Compte administrateur : admin / Admin@2026   (à changer après la 1re connexion)
-- ============================================================================

BEGIN;

-- Comptes utilisateurs ------------------------------------------------------
-- Les mots de passe sont hachés en PBKDF2-HMAC-SHA256 (120 000 itérations).
-- Pour générer un nouveau hachage : java -cp dev-fact.war com.devfact.security.PasswordTool "MotDePasse"
INSERT INTO app_user (username, password_hash, full_name, email, role)
VALUES ('admin',
        'pbkdf2-sha256$120000$tBB8Zr6ZddIhPANEQR30hA==$Qlt52S4+bbTESYievWuAM8zMiSKm5zz1BHXHzcbwzRw=',
        'Administrateur', 'admin@example.com', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_user (username, password_hash, full_name, email, role)
VALUES ('commercial',
        'pbkdf2-sha256$120000$FcyUJg8Ej8lYVmc4fylAJQ==$vCP+kJOB5uzSbfXQixJbcCu/ZRHZc2QLLozJ9BhIF8c=',
        'Service commercial', 'commercial@example.com', 'COMMERCIAL')
ON CONFLICT (username) DO NOTHING;

-- Paramètres société --------------------------------------------------------
INSERT INTO company_settings (id, company_name, address, city, phone, email, tax_id,
                              bank_details, currency, default_tax_rate,
                              quote_prefix, invoice_prefix, payment_terms_days,
                              quote_validity_days, footer_note)
VALUES (1,
        'Ma Société SARL',
        '12, rue des Palmiers',
        'Cotonou',
        '+229 00 00 00 00',
        'contact@masociete.bj',
        'IFU 0000000000000',
        'Banque : BOA Bénin - Compte n° 00000000000 - IBAN BJ00 0000 0000 0000 0000 0000 000',
        'XOF', 18.00, 'DEV', 'FAC', 30, 30,
        'Merci de votre confiance.')
ON CONFLICT (id) DO NOTHING;

COMMIT;
