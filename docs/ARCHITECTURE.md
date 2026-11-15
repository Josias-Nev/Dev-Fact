# Architecture technique — Dev-Fact

**Sommaire**

1. [Vue d'ensemble](#1-vue-densemble)
2. [Couches et packages](#2-couches-et-packages)
3. [Modèle de données](#3-modèle-de-données)
4. [Numérotation des documents](#4-numérotation-des-documents)
5. [Statuts et transitions](#5-statuts-et-transitions)
6. [Calcul des montants](#6-calcul-des-montants)
7. [Sécurité JAAS](#7-sécurité-jaas)
8. [Rendu PDF](#8-rendu-pdf)
9. [Choix techniques](#9-choix-techniques)
10. [Concurrence et intégrité](#10-concurrence-et-intégrité)

---

## 1. Vue d'ensemble

```
┌────────────┐   HTTP    ┌──────────────────────────────────────────────────────────┐
│ Navigateur │ ────────▶ │ WildFly 35 (Jakarta EE 10)                               │
└────────────┘           │                                                          │
                         │  EncodingFilter  (UTF-8)                                 │
                         │  AuthFilter      (session + jeton CSRF)                  │
                         │        │                                                 │
                         │        ▼                                                 │
                         │  Servlets  ──── forwarding ────▶  JSP + JSTL (WEB-INF)   │
                         │        │                                                 │
                         │        ▼                                                 │
                         │  Services (règles métier, transactions)                  │
                         │        │                    │                            │
                         │        ▼                    ▼                            │
                         │  DAO JDBC            ReportService ──▶ PDF (JasperReports)│
                         │        │                                                 │
                         └────────┼─────────────────────────────────────────────────┘
                                  │ JDBC (JNDI ou pilote embarqué)
                                  ▼
                         ┌──────────────────┐
                         │  PostgreSQL 16   │
                         └──────────────────┘
```

Aucune API REST, aucun framework front : le rendu est fait côté serveur (JSP), les actions passent
par des formulaires HTML `POST` protégés par un jeton CSRF.

---

## 2. Couches et packages

| Package | Responsabilité | Dépend de |
|---|---|---|
| `com.devfact.config` | Lecture de la configuration (propriétés, variables d'environnement) | — |
| `com.devfact.db` | `DataAccess` : source de données, connexions, transactions, `SqlWork` | config |
| `com.devfact.model` | Entités (`Client`, `Document`, `DocumentLine`, `CompanySettings`, `UserAccount`) et énumérations | util |
| `com.devfact.dao` | Requêtes SQL paramétrées, mapping `ResultSet` → objets | db, model |
| `com.devfact.service` | Règles métier : validation, totaux, numérotation, cycle de vie, conversion | dao, model |
| `com.devfact.security` | JAAS (`LoginModule`, `Configuration`, `CallbackHandler`, `Principal`), hachage, `AuthService` | dao, model |
| `com.devfact.report` | Compilation du modèle JasperReports et export PDF | model, util |
| `com.devfact.web` | Servlets, filtres, listener, aides de formulaires (`WebParams`, `FormSupport`) | service, report, security |
| `com.devfact.util` | Montants (`Money`), dates (`Dates`), montant en lettres (`AmountToWordsFr`), chaînes | — |

Règles de dépendance : `web → service → dao → db`, `security` et `report` sont appelés par `web`
et par `service` (pour `isInvoiced`, `loadSettings`). Aucune dépendance de `dao` vers `web`.

**Transactions.** `DataAccess.write(...)` ouvre une connexion, désactive l'auto-commit, exécute le
travail, commite ; toute `SQLException` ou `RuntimeException` (dont `ValidationException`)
provoque un `rollback`. `DataAccess.read(...)` utilise une connexion en lecture seule logique
(fermeture systématique en `try-with-resources`).

---

## 3. Modèle de données

```
app_user                      company_settings (1 ligne)
┌───────────────────┐         ┌──────────────────────────────┐
│ id                │         │ id = 1                       │
│ username  UNIQUE  │         │ company_name, address, city  │
│ password_hash     │         │ phone, email, tax_id         │
│ full_name, email  │         │ bank_details, footer_note    │
│ role (ADMIN|…)    │         │ currency, default_tax_rate   │
│ enabled           │         │ quote_prefix, invoice_prefix │
└─────────┬─────────┘         │ payment_terms_days, validity │
          │ created_by        └──────────────────────────────┘
          │
          ▼                                   client
   ┌──────────────────────────────────────────────────────────┐
   │ document                                                 │
   │ id                                                        │
   │ doc_type  DEVIS | FACTURE                                │──┐ source_document_id
   │ number    UNIQUE  (DEV-2026-0001)                        │◀─┘ (devis → facture)
   │ client_id ─────────────▶ client.id                       │
   │ status                                                │
   │ issue_date, due_date, subject, notes                  │
   │ tax_rate, subtotal, tax_amount, total, currency        │
   │ created_by ─────────────▶ app_user.id                  │
   └───────────┬──────────────────────────────────────────────┘
               │ 1..n
               ▼
   ┌───────────────────────────────┐        ┌──────────────────────────┐
   │ document_line                 │        │ document_sequence        │
   │ id, document_id               │        │ doc_type, period  (PK)   │
   │ position, description         │        │ last_value               │
   │ quantity, unit_price          │        └──────────────────────────┘
   │ line_total                    │
   └───────────────────────────────┘
```

Points importants :

- **Contraintes de cohérence en base** : `document_type_chk`, `document_status_chk` (le statut doit
  appartenir au type de document), `document_line_qty_chk` (quantité > 0),
  `document_line_price_chk` (prix ≥ 0), `company_settings_singleton` (`id = 1`).
- **Intégrité référentielle** : `document.client_id → client.id` (RESTRICT), `document_line.document_id
  → document.id` (`ON DELETE CASCADE`), `document.source_document_id → document.id`
  (`ON DELETE SET NULL`).
- **Horodatage** : déclencheurs `df_touch_updated_at()` sur `document` et `company_settings`.
- **Montants** : `NUMERIC(16,2)` — valeurs exactes, jamais de `double`. Le franc CFA s'affiche sans
  décimale (arrondi à l'unité), l'euro avec deux décimales.
- **Index** : `document (doc_type, status)`, `document (client_id)`, `document (issue_date DESC)`,
  `document_line (document_id, position)`, `client (lower(name))`.

---

## 4. Numérotation des documents

Format : `PRÉFIXE-ANNÉE-NUMÉRO` (préfixes configurables, `DEV`/`FAC` par défaut), numéro sur 4 chiffres.

L'attribution est **atomique** et réalisée dans la même transaction que l'insertion du document :

```sql
INSERT INTO document_sequence (doc_type, period, last_value) VALUES (?, ?, 1)
ON CONFLICT (doc_type, period)
DO UPDATE SET last_value = document_sequence.last_value + 1
RETURNING last_value;
```

`ON CONFLICT … DO UPDATE … RETURNING` garantit, sans verrou applicatif, que deux créations
simultanées obtiennent deux numéros distincts. Un compteur par type **et par année** assure la
remise à zéro annuelle.

Le numéro est attribué **à la création** (y compris pour un brouillon). Conséquence assumée :
supprimer un brouillon laisse un trou dans la séquence. Si votre réglementation impose une
numérotation strictement continue, supprimez la possibilité de suppression des brouillons
(`DocumentService.delete`) et attribuez le numéro lors du passage au statut « Émise ».

---

## 5. Statuts et transitions

| Type | Statuts possibles |
|---|---|
| Devis | `BROUILLON` · `ENVOYE` · `ACCEPTE` · `REFUSE` · `FACTURE` |
| Facture | `BROUILLON` · `EMISE` · `PAYEE` · `ANNULEE` |

Transitions autorisées (`DocumentStatus.canMoveTo`) :

```
DEVIS                              FACTURE
BROUILLON ──▶ ENVOYE               BROUILLON ──▶ EMISE
BROUILLON ──▶ ACCEPTE              EMISE     ──▶ PAYEE
BROUILLON ──▶ REFUSE               EMISE     ──▶ ANNULEE
ENVOYE    ──▶ ACCEPTE
ENVOYE    ──▶ REFUSE
ACCEPTE   ──▶ FACTURE   (uniquement via la conversion)
```

Règles complémentaires :

- seul un `BROUILLON` est modifiable ou supprimable (`Document.isEditable`) ;
- la conversion devis → facture exige un devis `ENVOYE` ou `ACCEPTE`, sans facture déjà liée
  (`document.source_document_id`) ; elle crée une facture `BROUILLON`, recopie les lignes, et passe
  le devis à `FACTURE` ;
- toute transition invalide lève une `ValidationException` affichée à l'utilisateur.

---

## 6. Calcul des montants

Pour chaque ligne : `line_total = round(quantity × unit_price)`, l'arrondi dépendant de la devise
(0 décimale pour XOF/XAF, 2 sinon), au demi supérieur (`HALF_UP`).

```
sous_total = Σ line_total
TVA        = round(sous_total × taux / 100)
TOTAL TTC  = sous_total + TVA
```

Les totaux sont toujours recalculés **côté serveur** (`DocumentService.computeTotals`) avant
enregistrement : l'aperçu JavaScript du formulaire n'est qu'un confort visuel. Le montant en lettres
est produit par `AmountToWordsFr` (règles françaises : « vingt et un », « soixante et onze »,
« quatre-vingts » mais « quatre-vingt mille », « deux cents » mais « deux cent mille »).

---

## 7. Sécurité JAAS

```
LoginServlet (POST /login)
      │  identifiant + mot de passe
      ▼
AuthService.authenticate(...)
      │  new LoginContext("DevFact", subject, callbackHandler, new DevFactJaasConfig())
      ▼
LoginContext.login()
      │
      ├─▶ DevFactCallbackHandler  → NameCallback / PasswordCallback
      ▼
DevFactLoginModule.login()
      │  AuthService.verifyCredentials(...)
      │        └─▶ UserDao.findByUsername  (PostgreSQL)
      │        └─▶ PasswordHasher.matches  (PBKDF2-HMAC-SHA256, comparaison à temps constant)
      ▼
commit()  → ajoute DevFactPrincipal(userId, username, fullName, role) au Subject
      ▼
AuthService  → UserSession (sérialisée en session HTTP), logout JAAS immédiat
```

Éléments notables :

- `DevFactJaasConfig` remplace un fichier `jaas.conf` : la configuration est un objet Java, testable
  et déployée avec l'application.
- `DevFactLoginModule.createAuthService()` est un point d'extension utilisé par les tests pour
  substituer la vérification en base.
- Les mots de passe ne sont **jamais** stockés en clair ; le format
  `pbkdf2-sha256$120000$sel$empreinte` permet d'augmenter le nombre d'itérations sans invalider les
  empreintes existantes.
- Le conteneur ne gère pas la sécurité déclarative (`<security-constraint>`) : l'application
  contrôle elle-même l'accès via `AuthFilter`, ce qui évite toute configuration `security-domain`
  côté WildFly et rend le WAR immédiatement déployable.
- Chaque requête `POST` porte un jeton CSRF stocké en session et comparé dans `AuthFilter` ;
  les redirections après connexion n'acceptent que des chemins internes (anti-*open redirect*).

---

## 8. Rendu PDF

Un **modèle unique** `src/main/resources/reports/document.jrxml` sert aux devis et aux factures :
le titre, les libellés de dates et les montants arrivent en paramètres (`P_DOC_TITLE`,
`P_DOC_DUE`, `P_TOTAL`…), les lignes via un `JRBeanCollectionDataSource` de `ReportLine`.

- Modèle **précompilé** `document.jasper` livré dans le WAR : chargement immédiat, sans dépendance
  au compilateur à l'exécution. Repli automatique sur la compilation du `.jrxml` si le `.jasper`
  est absent (ECJ étant embarqué, la compilation reste possible dans le serveur).
- Chargement **une seule fois** puis mise en cache statique (`ReportService.report()`), avec
  préchauffage au démarrage (`warmUp()`) pour échouer vite si le modèle est invalide.
- Régénération du modèle : `com.devfact.report.TemplateCompiler` (voir README, section 8).
- Les montants sont **pré-formatés** en Java (`Money`) et passés en chaînes : cela évite toute
  dépendance à la locale du serveur et garantit un rendu identique en HTML et en PDF.
- Aucune police externe : Helvetica (encodage Cp1252) suffit pour le français.
- Le fichier est produit en mémoire puis écrit avec `Content-Disposition: inline|attachment`
  (`?download=1`) ; les PDF ne sont pas stockés, ils sont régénérés à la demande.

AWT (`java.desktop`) est requis par JasperReports : la dépendance est déclarée dans
`WEB-INF/jboss-deployment-structure.xml`.

---

## 9. Choix techniques

| Choix | Justification |
|---|---|
| **JDBC + DAO, sans ORM** | périmètre réduit, requêtes explicites et performantes, aucun conflit de versions Hibernate/WildFly, WAR léger |
| **JSP + JSTL, pas de SPA** | rendu serveur, pas de build front, fonctionne hors ligne ; l'« habillage » est une feuille de style maison |
| **Sessions HTTP + filtre** | plus simple à déployer qu'une `security-domain` Elytron, tout en utilisant JAAS comme demandé |
| **Services transactionnels** | une seule classe porte les règles (numérotation + insertion + lignes) et donc l'atomicité |
| **Montants pré-formatés** | cohérence HTML/PDF, indépendance vis-à-vis de la locale du serveur |
| **Pilote JDBC dans le WAR** | déployable sans configuration serveur ; la source JNDI reste prioritaire si elle existe |
| **`ValidationException` non contrôlée** | provoque naturellement le rollback et permet de réafficher le formulaire avec les erreurs |

---

## 10. Concurrence et intégrité

- **Numérotation** : `ON CONFLICT … RETURNING` (atomique, sans verrou applicatif).
- **Création d'un document** : numéro + en-tête + lignes dans une seule transaction ; en cas
  d'erreur de validation, rien n'est écrit.
- **Conversion devis → facture** : vérification d'unicité (`source_document_id`) et mise à jour du
  statut du devis dans la même transaction ; un second clic ne peut pas créer deux factures.
- **Contraintes de base** en filet de sécurité (types, statuts, quantités, prix, unicité du numéro).
- **Connexions** : `try-with-resources` partout ; `DataAccess` restaure l'auto-commit après usage.
