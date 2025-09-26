# Etapa 1: Build con JDK 21
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Runtime con JDK 21
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Instalar dockerize para esperar a Postgres
RUN apt-get update && apt-get install -y wget \
    && wget https://github.com/jwilder/dockerize/releases/download/v0.6.1/dockerize-linux-amd64-v0.6.1.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-v0.6.1.tar.gz \
    && rm dockerize-linux-amd64-v0.6.1.tar.gz \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado de la etapa anterior
COPY --from=builder /app/target/siladocs-backend.jar siladocs-backend.jar

# Exponer el puerto (Render usará $PORT)
EXPOSE 8080

# Ejecutar con el perfil docker
ENTRYPOINT ["dockerize", "-wait", "tcp://postgres:5432", "-timeout", "30s", "--", "java", "-jar", "siladocs-backend.jar", "--spring.profiles.active=docker"]
