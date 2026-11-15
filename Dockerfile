# ============================================================================
#  Dev-Fact — image prête à l'emploi : WildFly 35 + application déployée
#  Construction : docker compose up --build   (ou docker build -t dev-fact .)
# ============================================================================

# --- Étape 1 : compilation du WAR -------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Les dépendances sont mises en cache dans une couche séparée
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline || true

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Étape 2 : serveur d'application ----------------------------------------
FROM quay.io/wildfly/wildfly:35.0.1.Final-jdk17

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=Africa/Porto-Novo

# Le pilote PostgreSQL est embarqué dans le WAR : aucune configuration
# de source de données n'est nécessaire, l'application lit ses paramètres
# dans les variables d'environnement (DEV_FACT_DB_URL, DEV_FACT_DB_USER...).
COPY --from=build /build/target/dev-fact.war /opt/jboss/wildfly/standalone/deployments/dev-fact.war

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
