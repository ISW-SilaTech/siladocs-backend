# ===================================================
# Etapa 1: La Cocina de Construcción (Builder 🏗️)
# ===================================================
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copia solo el pom.xml
COPY pom.xml .

# 2. Descarga todas las dependencias
# (Docker guardará esto en una capa separada)
RUN mvn dependency:go-offline

# 3. Copia el resto del código fuente
COPY src/ ./src/

# 4. Compila la aplicación (usará las dependencias ya descargadas)
RUN mvn clean package -DskipTests

## Etapa 1: Fin

# ===================================================
# Etapa 2: El Contenedor Final (Runtime 🚀)
# ===================================================
# 🔹 Usa JRE en lugar de JDK (más pequeño y seguro)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Instalar dockerize para esperar a Postgres
RUN apt-get update && apt-get install -y wget \
    && wget https://github.com/jwilder/dockerize/releases/download/v0.6.1/dockerize-linux-amd64-v0.6.1.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-v0.6.1.tar.gz \
    && rm dockerize-linux-amd64-v0.6.1.tar.gz \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado de la etapa anterior
# 🔹 La ruta del JAR ahora es más predecible
COPY --from=builder /app/target/siladocs-backend.jar siladocs-backend.jar

# Exponer el puerto
EXPOSE 8080

# Ejecutar con el perfil docker (tu comando original está perfecto)
ENTRYPOINT ["dockerize", "-wait", "tcp://postgres:5432", "-timeout", "30s", "--", "java", "-jar", "siladocs-backend.jar", "--spring.profiles.active=fabric"]
