# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar pom y código fuente
COPY pom.xml .
COPY src ./src

# Compilar y empaquetar el JAR
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Instalar wget y dockerize
RUN apk add --no-cache wget \
    && wget https://github.com/jwilder/dockerize/releases/download/v0.9.6/dockerize-alpine-linux-amd64-v0.9.6.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-alpine-linux-amd64-v0.9.6.tar.gz \
    && rm dockerize-alpine-linux-amd64-v0.9.6.tar.gz

# Copiar el JAR compilado de la etapa anterior
COPY --from=build /app/target/siladocs-backend-*.jar siladocs-backend.jar

# Exponer el puerto de Spring Boot
EXPOSE 8080

# Ejecutar con el perfil docker
ENTRYPOINT ["dockerize", "-wait", "tcp://postgres:5432", "-timeout", "30s", "java", "-jar", "siladocs-backend.jar", "--spring.profiles.active=docker"]
