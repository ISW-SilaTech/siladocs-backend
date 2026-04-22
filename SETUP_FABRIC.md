# 🚀 SilaDocs Backend - Guía de Setup (Hyperledger Fabric)

**Fecha:** Abril 2026  
**Versión:** 1.0  
**Descripción:** Guía completa para configurar, compilar y probar SilaDocs Backend con Hyperledger Fabric

---

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Estructura del Proyecto](#estructura-del-proyecto)
3. [Configuración Local](#configuración-local)
4. [Compilación](#compilación)
5. [Ejecución](#ejecución)
6. [Testing (Paso a Paso)](#testing-paso-a-paso)
7. [Troubleshooting](#troubleshooting)
8. [Archivos Clave Modificados](#archivos-clave-modificados)

---

## 🔧 Requisitos Previos

### Software Requerido

```bash
✓ Java 21 (JDK)
✓ Maven 3.8+
✓ Docker & Docker Compose
✓ Git
✓ Postman o curl (para testing manual)
```

### Verificar Instalación

```bash
# Java
java -version
# Debe mostrar: openjdk 21.x.x

# Maven
mvn -version
# Debe mostrar: Apache Maven 3.8+

# Docker
docker --version
docker-compose --version
```

---

## 📁 Estructura del Proyecto

```
siladocs-backend/
├── src/
│   ├── main/
│   │   ├── java/com/siladocs/
│   │   │   ├── application/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── DocumentController.java
│   │   │   │   │   └── HealthController.java ⭐ (NUEVO)
│   │   │   │   ├── service/
│   │   │   │   │   ├── BlockchainService.java ⭐ (REFACTORIZADO)
│   │   │   │   │   ├── SyllabusService.java ⭐ (REFACTORIZADO)
│   │   │   │   │   └── DocumentService.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── BlockchainFabricRequestDto.java ⭐ (NUEVO)
│   │   │   │   │   ├── BlockchainFabricResponseDto.java ⭐ (NUEVO)
│   │   │   │   │   └── ...
│   │   │   │   ├── exception/
│   │   │   │   │   └── BlockchainException.java ⭐ (NUEVO)
│   │   │   ├── infrastructure/
│   │   │   │   ├── config/
│   │   │   │   │   └── BlockchainConfig.java ⭐ (REFACTORIZADO)
│   │   │   │   ├── storage/
│   │   │   │   │   └── StorageService.java ⭐ (NUEVO)
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   ├── application-docker.yml
│   │   │   └── application-fabric.yml ⭐ (NUEVO)
│   └── test/
│       └── java/.../BlockchainServiceTest.java ⭐ (NUEVO)
├── pom.xml ⭐ (ACTUALIZADO - Web3j removido)
├── docker-compose.yml
└── Dockerfile
```

**Leyenda:**
- ⭐ = Archivos nuevos o refactorizados

---

## ⚙️ Configuración Local

### 1. Clonar Repositorio

```bash
cd ~/proyectos
git clone https://github.com/siladocs/siladocs-backend.git
cd siladocs-backend
```

### 2. Configurar Variables de Entorno

Crear archivo `.env` en la raíz del proyecto:

```bash
# .env
FABRIC_API_URL=http://127.0.0.1:8000
FABRIC_CONNECT_TIMEOUT=10000
FABRIC_READ_TIMEOUT=30000

JWT_SECRET=tu_secret_super_seguro_aqui_2026

MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=syllabi

POSTGRES_DB=siladocs
POSTGRES_USER=siladocs_user
POSTGRES_PASSWORD=siladocs_password
POSTGRES_PORT=5432
```

### 3. Configurar application-fabric.yml

Verificar que existe `/src/main/resources/application-fabric.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

blockchain:
  fabric:
    api:
      url: ${FABRIC_API_URL:http://127.0.0.1:8000}
      timeout:
        connect: ${FABRIC_CONNECT_TIMEOUT:10000}
        read: ${FABRIC_READ_TIMEOUT:30000}

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## 📦 Compilación

### Paso 1: Limpiar compilación anterior

```bash
mvn clean
```

**Output esperado:**
```
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------< com.siladocs:siladocs-backend >----------
[INFO] Building siladocs-backend 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]-----------
[INFO] Deleting /Users/.../siladocs-backend/target
[INFO] BUILD SUCCESS
```

### Paso 2: Compilar

```bash
mvn compile -DskipTests
```

**Output esperado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45.234 s
```

### Paso 3: Ejecutar Tests Unitarios

```bash
mvn test
```

**Output esperado:**
```
[INFO] Running com.siladocs.application.service.BlockchainServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

[INFO] BUILD SUCCESS
```

### Paso 4: Construir JAR

```bash
mvn package -DskipTests
```

**Output esperado:**
```
[INFO] Building jar: /path/to/siladocs-backend/target/siladocs-backend.jar
[INFO] BUILD SUCCESS
```

---

## 🚀 Ejecución

### Opción A: Docker Compose (RECOMENDADO para desarrollo)

#### 1. Iniciar servicios de soporte

```bash
# Desde la raíz del proyecto
docker-compose up -d postgresql minio
```

**Verificar servicios:**
```bash
docker-compose ps

OUTPUT:
NAME                COMMAND                      STATE           PORTS
postgresql          "docker-entrypoint.s..."     Up              5432/tcp
minio               "/usr/bin/minio server..."   Up              9000/tcp
```

#### 2. Esperar a que PostgreSQL esté listo

```bash
# Esperar ~10 segundos para que PostgreSQL inicie
sleep 10

# Verificar conexión
psql -h localhost -U siladocs_user -d siladocs -c "SELECT 1"
```

### Opción B: Ejecución Manual (Local)

#### 1. Asegurarse de que Fabric API esté corriendo

```bash
# En terminal separada, verificar que el middleware de Fabric esté activo
curl http://127.0.0.1:8000/health

# Output esperado:
# {"status": "healthy"}
```

#### 2. Ejecutar la aplicación Spring Boot

```bash
# Con perfil fabric activado
java -jar target/siladocs-backend.jar --spring.profiles.active=fabric
```

**Output esperado:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_|\__, | / / / /
 =========|_|==============|___/=/_/_/_/

2026-04-08 10:30:45.123 INFO: Starting SiladocsBackendApplication
2026-04-08 10:30:47.456 INFO: Started SiladocsBackendApplication in 2.345 seconds
Tomcat started on port(s): 8080
```

---

## 🧪 Testing (Paso a Paso)

### PARTE 1: Health Checks

#### Test 1.1: Verificar que la aplicación está activa

```bash
curl -X GET http://localhost:8080/health

# Output esperado (200 OK):
{
  "timestamp": "2026-04-08T10:35:22Z",
  "status": "UP",
  "application": "SilaDocs Backend",
  "fabric_available": true
}
```

#### Test 1.2: Verificar Fabric API

```bash
curl -X GET http://localhost:8080/health/fabric

# Output esperado (200 OK):
{
  "timestamp": "2026-04-08T10:35:25Z",
  "status": "UP",
  "service": "Hyperledger Fabric",
  "message": "API disponible"
}
```

#### Test 1.3: Health check detallado de Fabric

```bash
curl -X GET http://localhost:8080/health/fabric/detail

# Output esperado:
{
  "timestamp": "2026-04-08T10:35:30Z",
  "service": "Hyperledger Fabric",
  "details": "Fabric API Status Report\n===...\nAPI Available: ✅ YES\n..."
}
```

---

### PARTE 2: Autenticación (JWT)

#### Test 2.1: Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@siladocs.com",
    "password": "password123"
  }'

# Output esperado (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "admin@siladocs.com",
  "expiresIn": 86400000
}
```

**Guardar token para próximos tests:**
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### PARTE 3: Registro de Sílabo en Fabric (EL FLUJO PRINCIPAL)

#### Test 3.1: Crear un archivo de prueba

```bash
# Crear un sílabo de prueba
cat > /tmp/syllabus.pdf << 'EOF'
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
EOF
```

#### Test 3.2: Registrar sílabo con Fabric

```bash
# Asumir que hay un curso con ID = 1
curl -X POST http://localhost:8080/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: multipart/form-data" \
  -F "courseId=1" \
  -F "action=create" \
  -F "file=@/tmp/syllabus.pdf"

# Output esperado (201 CREATED):
{
  "syllabusId": 1,
  "courseId": 1,
  "version": 1,
  "fileHash": "abc123def456...",
  "fileUrl": "http://127.0.0.1:9000/syllabi/course-1/uuid-timestamp.pdf",
  "fabricTxId": "tx-12345-uuid",
  "status": "create",
  "createdAt": "2026-04-08T10:40:00Z"
}
```

**IMPORTANTE:** Este Test verifica el flujo COMPLETO:
1. ✅ Lectura del archivo
2. ✅ Cálculo del hash SHA-256
3. ✅ Upload a MinIO
4. ✅ Registro en Fabric
5. ✅ Persistencia en PostgreSQL

---

### PARTE 4: Documentos

#### Test 4.1: Subir documento

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/tmp/syllabus.pdf"

# Output esperado (201 CREATED):
{
  "id": 1,
  "fileName": "syllabus.pdf",
  "fileType": "application/pdf",
  "fileSize": 245,
  "hash": "abc123def456...",
  "uploadedAt": "2026-04-08T10:45:00Z"
}
```

#### Test 4.2: Obtener documento

```bash
curl -X GET http://localhost:8080/api/documents/1 \
  -H "Authorization: Bearer $TOKEN"

# Output esperado (200 OK):
{
  "id": 1,
  "fileName": "syllabus.pdf",
  ...
}
```

---

### PARTE 5: Error Handling

#### Test 5.1: Fabric API no disponible (simular error)

```bash
# Detener middleware de Fabric
docker stop fabric-api  # (si está en Docker)

# Intentar registrar sílabo
curl -X POST http://localhost:8080/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: multipart/form-data" \
  -F "courseId=1" \
  -F "action=update" \
  -F "file=@/tmp/syllabus.pdf"

# Output esperado (503 SERVICE UNAVAILABLE):
{
  "timestamp": "2026-04-08T10:50:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "No se pudo conectar con Fabric. ¿El middleware está activo?"
}
```

#### Test 5.2: Validación de entrada (archivo vacío)

```bash
curl -X POST http://localhost:8080/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: multipart/form-data" \
  -F "courseId=0" \
  -F "action=create" \
  -F "file=@/nonexistent.pdf"

# Output esperado (400 BAD REQUEST):
{
  "error": "Archivo vacío o nulo"
}
```

---

### PARTE 6: Logs y Debugging

#### Test 6.1: Ver logs detallados

```bash
# Si ejecutas con Spring Boot directamente
# En la terminal de ejecución verás:

2026-04-08 10:40:15.123 INFO  BlockchainService :
🔄 INICIANDO UPLOAD DE SÍLABO: courseId=1, user=admin@siladocs.com

2026-04-08 10:40:16.456 INFO  BlockchainService :
📝 Hash SHA-256 calculado: abc123def...

2026-04-08 10:40:17.789 INFO  BlockchainService :
☁️ Archivo subido a MinIO: http://127.0.0.1:9000/syllabi/...

2026-04-08 10:40:18.012 INFO  BlockchainService :
⛓️ REGISTRANDO EN HYPERLEDGER FABRIC...

2026-04-08 10:40:19.234 INFO  BlockchainService :
✅ FABRIC EXITOSO: txId=tx-12345-uuid

2026-04-08 10:40:20.456 INFO  BlockchainService :
✅ SÍLABO GUARDADO EN PostgreSQL: syllabusId=1
```

#### Test 6.2: Ver logs de BlockchainService en DEBUG

```bash
# Editar application-fabric.yml:
logging:
  level:
    com.siladocs.application.service.BlockchainService: DEBUG
    org.springframework.web.client: DEBUG
```

---

## 🐛 Troubleshooting

### Problema 1: "API de Fabric no está disponible"

**Síntomas:**
```
❌ Error: No se pudo conectar con Fabric. ¿El middleware está activo?
```

**Solución:**
```bash
# 1. Verificar que el middleware Python está corriendo
curl http://127.0.0.1:8000/health

# 2. Si no responde, iniciar el middleware
# (debe estar en otro terminal/contenedor)
cd ../siladocs-fabric-middleware
python -m uvicorn main:app --host 0.0.0.0 --port 8000

# 3. Verificar la URL en application-fabric.yml
# blockchain.fabric.api.url debe ser correcta
```

### Problema 2: "Timeout en solicitud a Fabric"

**Síntomas:**
```
org.springframework.web.client.SocketTimeoutException: Read timed out
```

**Solución:**
```bash
# 1. Aumentar timeouts en application-fabric.yml
blockchain:
  fabric:
    api:
      timeout:
        connect: 30000  # aumentar a 30s
        read: 60000     # aumentar a 60s

# 2. Verificar latencia de red
ping 127.0.0.1
```

### Problema 3: "PostgreSQL no está disponible"

**Síntomas:**
```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Solución:**
```bash
# 1. Verificar que PostgreSQL está corriendo
docker-compose ps | grep postgresql

# 2. Si no está, iniciarlo
docker-compose up -d postgresql

# 3. Esperar a que esté listo
sleep 15

# 4. Probar conexión
psql -h localhost -U siladocs_user -d siladocs -c "SELECT 1"
```

### Problema 4: "MinIO no está disponible"

**Síntomas:**
```
Error al subir archivo: Connection refused
```

**Solución:**
```bash
# 1. Iniciar MinIO
docker-compose up -d minio

# 2. Acceder a MinIO UI
# http://localhost:9000
# Usuario: minioadmin
# Contraseña: minioadmin

# 3. Crear bucket 'syllabi' si no existe
```

---

## 📝 Archivos Clave Modificados

### ELIMINADOS (Web3j)
- ❌ `org.web3j:core:4.10.0` (pom.xml)
- ❌ `Web3j web3j()` (BlockchainConfig.java)
- ❌ `Credentials credentials()` (BlockchainConfig.java)

### NUEVOS
- ✅ `BlockchainException.java` - Excepción personalizada
- ✅ `BlockchainFabricRequestDto.java` - Payload JSON
- ✅ `BlockchainFabricResponseDto.java` - Respuesta JSON
- ✅ `StorageService.java` - Gestión de MinIO
- ✅ `HealthController.java` - Health checks
- ✅ `BlockchainServiceTest.java` - Tests unitarios
- ✅ `application-fabric.yml` - Configuración Fabric

### REFACTORIZADOS
- 🔄 `BlockchainService.java` - Migramos a RestClient HTTP
- 🔄 `SyllabusService.java` - Nuevo flujo: Fabric → BD
- 🔄 `BlockchainConfig.java` - RestClient en lugar de Web3j
- 🔄 `pom.xml` - Removidas dependencias Web3j

---

## 📊 Resumen de Cambios

| Aspecto | Antes (Ethereum) | Después (Fabric) |
|---------|------------------|------------------|
| **Blockchain** | Ganache (Local Ethereum) | Hyperledger Fabric |
| **Cliente** | Web3j (Java) | RestClient (HTTP) |
| **Interacción** | Smart Contracts (Solidity) | API REST (Python) |
| **Transacción** | EthSendTransaction | HTTP POST JSON |
| **Validación** | On-chain (gas, nonces) | HTTP status codes |
| **Error Handling** | Web3j exceptions | BlockchainException |

---

## 🎉 Checklist Final

- [ ] Java 21 instalado
- [ ] Maven 3.8+ instalado
- [ ] Docker & Docker Compose instalado
- [ ] Repositorio clonado
- [ ] `.env` creado con variables
- [ ] `application-fabric.yml` verificado
- [ ] PostgreSQL iniciado (`docker-compose up -d postgresql`)
- [ ] MinIO iniciado (`docker-compose up -d minio`)
- [ ] Middleware Fabric corriendo en puerto 8000
- [ ] Compilación exitosa (`mvn package -DskipTests`)
- [ ] Tests pasaron (`mvn test`)
- [ ] Aplicación inicia correctamente
- [ ] Health check responde (GET `/health`)
- [ ] Fabric API disponible (GET `/health/fabric`)
- [ ] Test de login exitoso (POST `/auth/login`)
- [ ] Test de sílabo exitoso (POST `/api/syllabi/upload`)

---

## 📞 Soporte

Si encuentras problemas:

1. Revisa los logs: `tail -f /path/to/siladocs-backend/logs/app.log`
2. Verifica conectividad: `curl -v http://127.0.0.1:8000/health`
3. Consulta la sección [Troubleshooting](#troubleshooting)
4. Abre un issue en GitHub con detalles

---

**Documentación completa actualizada: Abril 2026**
