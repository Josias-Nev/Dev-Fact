# Guide de déploiement — Dev-Fact

Ce document décrit les trois façons de déployer l'application, les vérifications à effectuer,
les sauvegardes et le dépannage.

**Sommaire**

1. [Prérequis](#1-prérequis)
2. [Option A — Docker Compose (le plus simple)](#2-option-a--docker-compose-le-plus-simple)
3. [Option B — WildFly et PostgreSQL installés sur le serveur](#3-option-b--wildfly-et-postgresql-installés-sur-le-serveur)
4. [Option C — Source de données JNDI gérée par WildFly](#4-option-c--source-de-données-jndi-gérée-par-wildfly)
5. [Vérifications après déploiement](#5-vérifications-après-déploiement)
6. [Sauvegarde et restauration](#6-sauvegarde-et-restauration)
7. [Mise à jour de l'application](#7-mise-à-jour-de-lapplication)
8. [Dépannage](#8-dépannage)
9. [Durcissement pour la production](#9-durcissement-pour-la-production)

---

## 1. Prérequis

| Composant | Version | Remarque |
|---|---|---|
| JDK | 17 ou supérieur | WildFly 35 exige Java 17 |
| WildFly | 35.x | Jakarta EE 10 ; les versions 36+ (Jakarta EE 11) fonctionnent également |
| PostgreSQL | 14 ou supérieur | testé sur PostgreSQL 16 |
| Maven | 3.8+ | uniquement pour construire le WAR |
| Docker / Docker Compose | 20+ | option A uniquement |

Le port applicatif par défaut est **8080** (contexte `/dev-fact`).

---

## 2. Option A — Docker Compose (le plus simple)

### 2.1 Démarrage

```bash
cp .env.example .env          # facultatif : y placer le mot de passe de la base
docker compose up --build
```

La pile démarre deux conteneurs :

| Conteneur | Image | Rôle |
|---|---|---|
| `devfact-db` | `postgres:16-alpine` | base `devfact`, scripts SQL exécutés au premier lancement |
| `devfact-app` | construction locale (`Dockerfile`) | WildFly 35 + `dev-fact.war` déployé |

### 2.2 Accès

- Application : <http://localhost:8080/dev-fact/>
- Console d'administration WildFly : <http://localhost:9990> (aucun compte créé par défaut)

### 2.3 Jeu de données de démonstration

Les scripts présents dans `database/` sont exécutés automatiquement au **premier** démarrage
(`01_schema.sql`, `02_seed.sql`). Pour ajouter des exemples :

```bash
docker compose exec -T db psql -U devfact -d devfact < database/optional/03_demo_data.sql
```

### 2.4 Exploitation

```bash
docker compose logs -f app     # journaux de WildFly (déploiement, erreurs)
docker compose ps              # état des services
docker compose down            # arrêt (les données sont conservées)
docker compose down -v         # arrêt + suppression du volume de données
```

---

## 3. Option B — WildFly et PostgreSQL installés sur le serveur

### 3.1 Base de données

```bash
sudo -u postgres psql <<'SQL'
CREATE ROLE devfact LOGIN PASSWORD 'devfact';
CREATE DATABASE devfact OWNER devfact;
SQL

psql "postgresql://devfact:devfact@localhost:5432/devfact" -v ON_ERROR_STOP=1 -f database/01_schema.sql
psql "postgresql://devfact:devfact@localhost:5432/devfact" -v ON_ERROR_STOP=1 -f database/02_seed.sql
```

### 3.2 Construction du WAR

```bash
mvn clean package
# → target/dev-fact.war
```

> Le pilote PostgreSQL est déjà inclus dans le WAR : aucune bibliothèque à copier dans WildFly.

### 3.3 Configuration de la connexion

Deux possibilités, au choix :

**a. Variables d'environnement (le plus direct).** Éditez `bin/standalone.conf` et ajoutez :

```bash
JAVA_OPTS="$JAVA_OPTS -Ddevfact.db.url=jdbc:postgresql://localhost:5432/devfact"
JAVA_OPTS="$JAVA_OPTS -Ddevfact.db.user=devfact"
JAVA_OPTS="$JAVA_OPTS -Ddevfact.db.password=devfact"
```

Les propriétés système sont prioritaires ; sinon l'application lit `DEV_FACT_DB_*`, puis applique
ses valeurs par défaut.

**b. Source de données JNDI** : voir [option C](#4-option-c--source-de-données-jndi-gérée-par-wildfly).

### 3.4 Déploiement

```bash
cp target/dev-fact.war $WILDFLY_HOME/standalone/deployments/
# WildFly détecte le fichier et crée dev-fact.war.deployed (ou .failed)
```

Ou en ligne de commande :

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect --command="deploy target/dev-fact.war"
```

### 3.5 Accès

- <http://serveur:8080/dev-fact/>
- Comptes initiaux : `admin` / `Admin@2026` et `commercial` / `Demo@2026`.

Pour servir l'application à la racine (`/`), déployez le fichier sous le nom `ROOT.war`.

---

## 4. Option C — Source de données JNDI gérée par WildFly

Utile en production : le pool de connexions est géré par le serveur et partagé.

```bash
# 1. Récupérer le pilote JDBC
curl -LO https://jdbc.postgresql.org/download/postgresql-42.7.4.jar
cp postgresql-42.7.4.jar /tmp/

# 2. Serveur arrêté, appliquer la configuration
$WILDFLY_HOME/bin/jboss-cli.sh --file=docker/wildfly-datasource.cli

# 3. Démarrer et déployer
$WILDFLY_HOME/bin/standalone.sh
cp target/dev-fact.war $WILDFLY_HOME/standalone/deployments/
```

Le script crée le module `org.postgresql` et la source de données
`java:jboss/datasources/DevFactDS` (nom surchargeable avec `DEV_FACT_DS`).

Au démarrage, l'application affiche dans les journaux la source retenue :

```
INFO  [com.devfact.db.DataAccess] Source de données initialisée : JNDI java:jboss/datasources/DevFactDS
```

ou, en l'absence de JNDI :

```
INFO  [com.devfact.db.DataAccess] Source de données initialisée : JDBC direct jdbc:postgresql://…
```

---

## 5. Vérifications après déploiement

| Contrôle | Comment | Attendu |
|---|---|---|
| Déploiement | `standalone/log/server.log` | `Deployed "dev-fact.war"` |
| Base joignable | journaux au démarrage | `Base de données joignable` |
| Modèle PDF | journaux au démarrage | `Modèle JasperReports compilé.` |
| Page de connexion | `curl -I http://localhost:8080/dev-fact/login` | `HTTP/1.1 200` |
| Redirection d'accès | `curl -I http://localhost:8080/dev-fact/documents` | `302` vers `/login` |
| Table de test | `psql -c "select count(*) from document;"` | pas d'erreur |

Un contrôle rapide en ligne de commande :

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/dev-fact/login     # 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/dev-fact/documents # 302
```

---

## 6. Sauvegarde et restauration

```bash
# Sauvegarde logique
pg_dump -Fc -h localhost -U devfact devfact > devfact-$(date +%F).dump

# Restauration
pg_restore -h localhost -U devfact -d devfact --clean --if-exists devfact-2026-11-15.dump
```

Avec Docker :

```bash
# Sauvegarde
docker compose exec -T db pg_dump -Fc -U devfact devfact > devfact-$(date +%F).dump

# Restauration
cat devfact-2026-11-15.dump | docker compose exec -T db pg_restore -U devfact -d devfact --clean --if-exists
```

Les PDF ne sont pas stockés : ils sont régénérés à la demande depuis la base. Une sauvegarde de la
base suffit donc à tout reconstituer.

---

## 7. Mise à jour de l'application

1. Sauvegarder la base (section 6).
2. `mvn clean package`
3. Remplacer le WAR dans `standalone/deployments/` (WildFly redéploie automatiquement) :

```bash
cp target/dev-fact.war $WILDFLY_HOME/standalone/deployments/dev-fact.war
```

Avec Docker : `docker compose up --build -d`.

Les évolutions de schéma doivent être appliquées manuellement (scripts SQL versionnés dans
`database/`) ; il n'y a pas d'outil de migration automatique dans cette version.

---

## 8. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `Deployment dev-fact.war failed` avec `DependencyResolutionException` | module JDK `java.desktop` introuvable | vérifier la version de WildFly (35+), ou retirer la dépendance de `WEB-INF/jboss-deployment-structure.xml` si votre distribution l'expose déjà |
| Page de connexion affiche « La base de données est injoignable » | URL, identifiants ou service PostgreSQL | tester `psql "postgresql://devfact:devfact@localhost:5432/devfact"`, contrôler `DEV_FACT_DB_*` |
| `FATAL: password authentication failed` | mot de passe erroné | mettre à jour `DEV_FACT_DB_PASSWORD` (ou le fichier `.env`) |
| `Identifiant ou mot de passe incorrect` avec les comptes par défaut | scripts `02_seed.sql` non exécutés | exécuter `02_seed.sql` |
| « Génération du PDF impossible » | police/AWT ou modèle JasperReports | consulter la trace dans `server.log` ; vérifier que `java.desktop` est accessible |
| `Jeton de sécurité invalide ou expiré` (403) | session expirée ou page laissée ouverte trop longtemps | se reconnecter |
| Aucun devis affiché | aucun document en base | utiliser « + Nouveau devis » |
| `planned refresh` / déploiement en boucle | WAR partiellement copié | s'assurer que la copie est complète (`ls -l`), ou déployer via `jboss-cli.sh` |
| Erreurs d'accents (Ã©) | encodage du client SQL | `pg_dump`/`psql` avec `PGCLIENTENCODING=UTF8` |

Consultation des journaux :

```bash
tail -f $WILDFLY_HOME/standalone/log/server.log
docker compose logs -f app
```

---

## 9. Durcissement pour la production

1. **Changer les mots de passe par défaut** (`PasswordTool`, section 6 du README) et supprimer le
   compte `commercial` de démonstration s'il n'est pas utilisé.
2. **HTTPS** : placer WildFly derrière un reverse proxy (Nginx, Apache, Traefik) et activer
   `secure="true"` sur le cookie de session via `<cookie-config><secure>true</secure></cookie-config>`
   dans `web.xml`.
3. **Base de données** : utilisateur dédié avec droits limités à la base `devfact`, sauvegardes
   quotidiennes, `pg_hba.conf` restreint au réseau applicatif.
4. **Sessions** : adapter `session-timeout` (30 minutes par défaut) à votre politique.
5. **Journalisation** : conserver `server.log` et le faire tourner (`jboss-logmanager`).
6. **Réseau** : n'exposer que les ports utiles (8080 derrière proxy, 9990 restreint).
