# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copiar pom y código fuente
COPY pom.xml .
COPY src ./src

# Compilar y empaquetar el JAR
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Instalar wget y dockerize
RUN apt-get update && apt-get install -y wget \
    && wget https://github.com/jwilder/dockerize/releases/download/v0.9.6/dockerize-linux-amd64-v0.9.6.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-v0.9.6.tar.gz \
    && rm dockerize-linux-amd64-v0.9.6.tar.gz

# Copiar el JAR compilado de la etapa anterior
COPY --from=builder /app/target/siladocs-backend-*.jar siladocs-backend.jar

# Exponer el puerto (Render usará $PORT)
EXPOSE 8080

# Ejecutar con el perfil docker
ENTRYPOINT ["dockerize", "-wait", "tcp://postgres:5432", "-timeout", "30s", "java", "-jar", "siladocs-backend.jar", "--spring.profiles.active=docker"]
