# Imagen de la aplicacion, pensada para que el proyecto se levante con
# "docker compose up --build -d" sin instalar un JDK 7/8 en la maquina:
# el JDK y el Maven viejos quedan adentro de la imagen.
#
# Build (etapa 1): Maven 3.9 + JDK 8 (Eclipse Temurin). Se usa JDK 8 a
# proposito porque los plugins de Maven que pide el pom (maven-war-plugin
# 2.4, tomcat7-maven-plugin 2.2, ~2012-2014) rompen con el module system
# de JDK 9+ (InaccessibleObjectException al generar el WAR). Bajo JDK 8
# no existe ese problema.
FROM maven:3.9-eclipse-temurin-8 AS build

WORKDIR /build

# Primero solo lo necesario para resolver dependencias (mejor cache de
# capas: un cambio en src/ no obliga a re-descargar todo Maven Central).
COPY pom.xml .
COPY libs ./libs
RUN mvn -B dependency:go-offline

# jdbc.url se pisa en build time: dentro de la red de docker compose el
# host de Oracle es el nombre del servicio ("oracle-db"), no "localhost".
ARG JDBC_URL=jdbc:oracle:thin:@oracle-db:1521/XEPDB1

COPY src ./src
RUN mvn -B package -DskipTests -Djdbc.url=${JDBC_URL}

# Runtime (etapa 2): Tomcat 8.5 + JDK 8, igual de "viejo" que pide el
# pom.xml (servlet-api 2.5 / JSP), sin Maven ni el JDK de build adentro.
FROM tomcat:8.5-jdk8-temurin-jammy

# El WAR se despliega con el mismo nombre de contexto que documenta el
# README (/arquita-legacy), igual que con tomcat7-maven-plugin.
COPY --from=build /build/target/arquita-legacy.war /usr/local/tomcat/webapps/arquita-legacy.war

EXPOSE 8080
