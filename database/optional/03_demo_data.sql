-- ============================================================================
--  Dev-Fact - Jeu de données de démonstration (facultatif)
--  À exécuter APRÈS 01_schema.sql et 02_seed.sql :
--     psql -U devfact -d devfact -f optional/03_demo_data.sql
-- ============================================================================

BEGIN;

INSERT INTO client (name, contact_name, email, phone, address, city, tax_id)
VALUES ('Société Ganhi Distribution', 'M. Koffi', 'achats@ganhi.example', '+229 97 00 00 01',
        'Zone industrielle, lot 42', 'Cotonou', 'IFU 3201900000123'),
       ('Cabinet Afrique Conseil', 'Mme Adjo', 'contact@afrique-conseil.example', '+229 96 00 00 02',
        'Immeuble Le Diplomate, 3e étage', 'Abomey-Calavi', 'IFU 4202000000456');

INSERT INTO document (doc_type, number, client_id, status, issue_date, due_date, subject,
                      notes, tax_rate, subtotal, tax_amount, total, currency, created_by)
VALUES ('DEVIS', 'DEV-' || to_char(CURRENT_DATE, 'YYYY') || '-0001',
        (SELECT id FROM client ORDER BY id LIMIT 1), 'ENVOYE',
        CURRENT_DATE, CURRENT_DATE + 30, 'Fourniture et installation de 5 postes informatiques',
        'Validité 30 jours. Livraison sous 15 jours ouvrés.', 18.00, 2750000, 495000, 3245000, 'XOF',
        (SELECT id FROM app_user WHERE username = 'admin'));

INSERT INTO document_line (document_id, position, description, quantity, unit_price, line_total)
SELECT d.id, 1, 'Ordinateur portable 15" - 16 Go RAM, SSD 512 Go', 5, 450000, 2250000
FROM document d WHERE d.number = 'DEV-' || to_char(CURRENT_DATE, 'YYYY') || '-0001'
UNION ALL
SELECT d.id, 2, 'Station d''accueil - écran 24", clavier et souris', 5, 100000, 500000
FROM document d WHERE d.number = 'DEV-' || to_char(CURRENT_DATE, 'YYYY') || '-0001';

INSERT INTO document (doc_type, number, client_id, status, issue_date, due_date, subject,
                      notes, tax_rate, subtotal, tax_amount, total, currency, created_by)
VALUES ('FACTURE', 'FAC-' || to_char(CURRENT_DATE, 'YYYY') || '-0001',
        (SELECT id FROM client ORDER BY id DESC LIMIT 1), 'EMISE',
        CURRENT_DATE, CURRENT_DATE + 15, 'Prestation de conseil - novembre',
        'Paiement par virement bancaire.', 18.00, 800000, 144000, 944000, 'XOF',
        (SELECT id FROM app_user WHERE username = 'admin'));

INSERT INTO document_line (document_id, position, description, quantity, unit_price, line_total)
SELECT d.id, 1, 'Journée de conseil en organisation', 4, 200000, 800000
FROM document d WHERE d.number = 'FAC-' || to_char(CURRENT_DATE, 'YYYY') || '-0001';

INSERT INTO document_sequence (doc_type, period, last_value)
VALUES ('DEVIS', EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, 1),
       ('FACTURE', EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, 1)
ON CONFLICT (doc_type, period) DO UPDATE SET last_value = EXCLUDED.last_value;

COMMIT;
