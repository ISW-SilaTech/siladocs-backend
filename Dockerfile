# Etapa 1: Build
FROM mirror.gcr.io/library/maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM mirror.gcr.io/library/eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/target/siladocs-backend-*.jar siladocs-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "siladocs-backend.jar"]

# Copiar el JAR compilado de la etapa anterior
COPY --from=builder /app/target/siladocs-backend-*.jar siladocs-backend.jar

# Exponer el puerto (Render usará $PORT)
EXPOSE 8080

# Ejecutar con el perfil docker
ENTRYPOINT ["dockerize", "-wait", "tcp://postgres:5432", "-timeout", "30s", "java", "-jar", "siladocs-backend.jar", "--spring.profiles.active=docker"]
