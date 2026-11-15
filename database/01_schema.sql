-- ============================================================================
--  Dev-Fact - Gestion des devis et factures
--  Schéma PostgreSQL 14+
--  Exécution : psql -U devfact -d devfact -f 01_schema.sql
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- Fonction utilitaire : mise à jour automatique de updated_at
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION df_touch_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- Utilisateurs de l'application (authentification JAAS)
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(120),
    role          VARCHAR(20)  NOT NULL DEFAULT 'COMMERCIAL',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    CONSTRAINT app_user_role_chk CHECK (role IN ('ADMIN', 'COMMERCIAL'))
);

-- ---------------------------------------------------------------------------
-- Clients
-- ---------------------------------------------------------------------------
CREATE TABLE client (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    contact_name VARCHAR(120),
    email        VARCHAR(120),
    phone        VARCHAR(40),
    address      VARCHAR(255),
    city         VARCHAR(80),
    tax_id       VARCHAR(40),          -- IFU / NIF / numéro de TVA
    notes        TEXT,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX client_name_idx    ON client (lower(name));
CREATE INDEX client_active_idx  ON client (active);

-- ---------------------------------------------------------------------------
-- Paramètres de la société (une seule ligne, id = 1)
-- ---------------------------------------------------------------------------
CREATE TABLE company_settings (
    id                  SMALLINT PRIMARY KEY DEFAULT 1,
    company_name        VARCHAR(150) NOT NULL,
    address             VARCHAR(255),
    city                VARCHAR(80),
    phone               VARCHAR(40),
    email               VARCHAR(120),
    tax_id              VARCHAR(40),
    bank_details        TEXT,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'XOF',
    default_tax_rate    NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    quote_prefix        VARCHAR(10)  NOT NULL DEFAULT 'DEV',
    invoice_prefix      VARCHAR(10)  NOT NULL DEFAULT 'FAC',
    payment_terms_days  INTEGER      NOT NULL DEFAULT 30,
    quote_validity_days INTEGER      NOT NULL DEFAULT 30,
    footer_note         TEXT,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT company_settings_singleton CHECK (id = 1),
    CONSTRAINT company_settings_tax_chk  CHECK (default_tax_rate >= 0 AND default_tax_rate <= 100)
);

CREATE TRIGGER company_settings_touch
    BEFORE UPDATE ON company_settings
    FOR EACH ROW EXECUTE FUNCTION df_touch_updated_at();

-- ---------------------------------------------------------------------------
-- Documents : devis et factures
-- ---------------------------------------------------------------------------
CREATE TABLE document (
    id                 BIGSERIAL PRIMARY KEY,
    doc_type           VARCHAR(10)  NOT NULL,
    number             VARCHAR(30)  NOT NULL UNIQUE,
    client_id          BIGINT       NOT NULL REFERENCES client (id),
    status             VARCHAR(20)  NOT NULL DEFAULT 'BROUILLON',
    issue_date         DATE         NOT NULL DEFAULT CURRENT_DATE,
    due_date           DATE,
    subject            VARCHAR(255),
    notes              TEXT,
    tax_rate           NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    subtotal           NUMERIC(16,2) NOT NULL DEFAULT 0,
    tax_amount         NUMERIC(16,2) NOT NULL DEFAULT 0,
    total              NUMERIC(16,2) NOT NULL DEFAULT 0,
    currency           VARCHAR(3)   NOT NULL DEFAULT 'XOF',
    source_document_id BIGINT       REFERENCES document (id) ON DELETE SET NULL,
    created_by         BIGINT       REFERENCES app_user (id),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT document_type_chk CHECK (doc_type IN ('DEVIS', 'FACTURE')),
    CONSTRAINT document_status_chk CHECK (
        (doc_type = 'DEVIS'   AND status IN ('BROUILLON', 'ENVOYE', 'ACCEPTE', 'REFUSE', 'FACTURE'))
     OR (doc_type = 'FACTURE' AND status IN ('BROUILLON', 'EMISE', 'PAYEE', 'ANNULEE'))
    ),
    CONSTRAINT document_tax_chk CHECK (tax_rate >= 0 AND tax_rate <= 100)
);

CREATE INDEX document_type_status_idx ON document (doc_type, status);
CREATE INDEX document_client_idx      ON document (client_id);
CREATE INDEX document_issue_date_idx  ON document (issue_date DESC);
CREATE INDEX document_source_idx      ON document (source_document_id);

CREATE TRIGGER document_touch
    BEFORE UPDATE ON document
    FOR EACH ROW EXECUTE FUNCTION df_touch_updated_at();

-- ---------------------------------------------------------------------------
-- Lignes de document
-- ---------------------------------------------------------------------------
CREATE TABLE document_line (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT        NOT NULL REFERENCES document (id) ON DELETE CASCADE,
    position    INTEGER       NOT NULL DEFAULT 1,
    description VARCHAR(255)  NOT NULL,
    quantity    NUMERIC(12,2) NOT NULL DEFAULT 1,
    unit_price  NUMERIC(16,2) NOT NULL DEFAULT 0,
    line_total  NUMERIC(16,2) NOT NULL DEFAULT 0,
    CONSTRAINT document_line_qty_chk   CHECK (quantity > 0),
    CONSTRAINT document_line_price_chk CHECK (unit_price >= 0)
);

CREATE INDEX document_line_doc_idx ON document_line (document_id, position);

-- ---------------------------------------------------------------------------
-- Compteur de numérotation (une séquence logique par type et par année)
-- ---------------------------------------------------------------------------
CREATE TABLE document_sequence (
    doc_type   VARCHAR(10) NOT NULL,
    period     SMALLINT    NOT NULL,
    last_value INTEGER     NOT NULL DEFAULT 0,
    PRIMARY KEY (doc_type, period)
);

COMMIT;
