#!/bin/bash

################################################################################
#
# 🎉 SILADOCS BACKEND - REFACTORING COMPLETADO
#
# Migración de Ethereum (Web3j) → Hyperledger Fabric (REST API)
# 
# Genera Abril 2026
# Versión: 1.0-Fabric
#
################################################################################

cat << 'EOF'

    ███████╗██╗██╗      █████╗ ██████╗  ██████╗  ██████╗███████╗
    ██╔════╝██║██║     ██╔══██╗██╔══██╗██╔═══██╗██╔════╝██╔════╝
    ███████╗██║██║     ███████║██║  ██║██║   ██║██║     ███████╗
    ╚════██║██║██║     ██╔══██║██║  ██║██║   ██║██║     ╚════██║
    ███████║██║███████╗██║  ██║██████╔╝╚██████╔╝╚██████╗███████║
    ╚══════╝╚═╝╚══════╝╚═╝  ╚═╝╚═════╝  ╚═════╝  ╚═════╝╚══════╝

    🔗 Backend Hyperledger Fabric (v1.0)
    
================================================================================
EOF

echo ""
echo "📊 RESUMEN DE CAMBIOS"
echo "================================================================================"
echo ""
echo "✅ COMPLETADO: Migración de Ethereum → Hyperledger Fabric"
echo ""
echo "   De:"
echo "   ├─ Blockchain: Ganache (Ethereum local)"
echo "   ├─ Cliente: Web3j (Java native)"
echo "   ├─ Transacciones: Smart Contracts compilados"
echo "   └─ Criptografía: ECDSA"
echo ""
echo "   A:"
echo "   ├─ Blockchain: Hyperledger Fabric"
echo "   ├─ Cliente: RestClient (Spring 3.x HTTP)"
echo "   ├─ Transacciones: API REST JSON"
echo "   └─ Criptografía: Delegada a Fabric"
echo ""
echo "================================================================================"
echo ""

echo "📁 ARCHIVOS MODIFICADOS"
echo "================================================================================"

cat << 'EOF'
CREADOS (13 NUEVOS):
  ✨ src/main/java/com/siladocs/application/exception/BlockchainException.java
  ✨ src/main/java/com/siladocs/application/dto/BlockchainFabricRequestDto.java
  ✨ src/main/java/com/siladocs/application/dto/BlockchainFabricResponseDto.java
  ✨ src/main/java/com/siladocs/application/controller/HealthController.java
  ✨ src/main/java/com/siladocs/infrastructure/storage/StorageService.java
  ✨ src/test/java/com/siladocs/application/service/BlockchainServiceTest.java
  ✨ src/main/resources/application-fabric.yml
  ✨ SETUP_FABRIC.md (Guía completa)
  ✨ test_siladocs.sh (Tests automatizados)
  ✨ SilaDocs_Fabric_Postman.json (Colección Postman)

REFACTORIZADOS (4):
  🔄 src/main/java/com/siladocs/application/service/BlockchainService.java
  🔄 src/main/java/com/siladocs/application/service/SyllabusService.java
  🔄 src/main/java/com/siladocs/infrastructure/config/BlockchainConfig.java
  🔄 pom.xml (Removida dependencia Web3j)

ELIMINADOS (1):
  ❌ org.web3j:core:4.10.0 (del pom.xml)

EOF

echo ""
echo "================================================================================"
echo ""
echo "🚀 PASO A PASO PARA PROBAR"
echo "================================================================================"
echo ""

cat << 'EOF'

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 1: PREPARAR ENTORNO (5 minutos)                                     ║
╠════════════════════════════════════════════════════════════════════════════╣

# 1.1 - Instalar requisitos (si no los tienes)
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk maven docker.io docker-compose

# 1.2 - Verificar instalación
java -version        # Debe ser 21.x
mvn -version         # Debe ser 3.8+
docker --version     # Docker instalado

# 1.3 - Navegar al proyecto
cd ~/proyectos/siladocs-backend

# 1.4 - Crear archivo .env
cat > .env << 'ENVFILE'
FABRIC_API_URL=http://127.0.0.1:8000
FABRIC_CONNECT_TIMEOUT=10000
FABRIC_READ_TIMEOUT=30000

JWT_SECRET=tu_secret_seguro_2026

MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=syllabi

POSTGRES_DB=siladocs
POSTGRES_USER=siladocs_user
POSTGRES_PASSWORD=siladocs_password
POSTGRES_PORT=5432
ENVFILE

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 2: COMPILAR (3 minutos)                                             ║
╠════════════════════════════════════════════════════════════════════════════╣

# 2.1 - Limpiar compilaciones anteriores
mvn clean

# 2.2 - Compilar sin tests (rápido)
mvn compile -DskipTests

# 2.3 - Compilar CON tests unitarios
mvn test

# 2.4 - Crear JAR ejecutable
mvn package -DskipTests

# Verificar que el JAR fue creado
ls -lh target/siladocs-backend.jar

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 3: INICIAR SERVICIOS (Opción A: Docker Compose)                    ║
╠════════════════════════════════════════════════════════════════════════════╣

# 3.1 - Iniciar PostgreSQL y MinIO
docker-compose up -d postgresql minio

# 3.2 - Verificar que estén activos
docker-compose ps

# 3.3 - IMPORTANTE: El middleware Fabric DEBE estar corriendo
# En OTRA terminal, ejecutar:
cd ../siladocs-fabric-middleware
python -m uvicorn main:app --host 0.0.0.0 --port 8000

# Verificar disponibilidad:
curl http://127.0.0.1:8000/health

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 4: EJECUTAR APLICACIÓN                                             ║
╠════════════════════════════════════════════════════════════════════════════╣

# 4.1 - Ejecutar SilaDocs Backend (terminal 1)
java -jar target/siladocs-backend.jar \
  --spring.profiles.active=fabric \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/siladocs \
  --spring.datasource.username=siladocs_user \
  --spring.datasource.password=siladocs_password

# 4.2 - Esperar a que inicie (busca este mensaje):
# "Started SiladocsBackendApplication in X.XXX seconds"
# Tomcat started on port(s): 8080

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 5: TESTING BÁSICO (En otra terminal)                               ║
╠════════════════════════════════════════════════════════════════════════════╣

# 5.1 - Health Check General
curl http://localhost:8080/health

# OUTPUT ESPERADO:
# {
#   "timestamp": "2026-04-08T10:00:00Z",
#   "status": "UP",
#   "application": "SilaDocs Backend",
#   "fabric_available": true
# }

# 5.2 - Health Check Fabric
curl http://localhost:8080/health/fabric

# OUTPUT ESPERADO (200 OK):
# {
#   "status": "UP",
#   "service": "Hyperledger Fabric",
#   "message": "API disponible"
# }

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 6: LOGIN Y OBTENER TOKEN JWT                                       ║
╠════════════════════════════════════════════════════════════════════════════╣

# 6.1 - Registrar usuario (primera vez)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@siladocs.com",
    "password": "password123"
  }'

# 6.2 - Login
RESPONSE=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@siladocs.com",
    "password": "password123"
  }')

TOKEN=$(echo "$RESPONSE" | jq -r '.token')
echo "Token: $TOKEN"

# 6.3 - Guardar token en variable de entorno
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI..."

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 7: PRUEBA PRINCIPAL - REGISTRAR SÍLABO EN FABRIC 🔑                 ║
╠════════════════════════════════════════════════════════════════════════════╣

# 7.1 - Crear archivo de prueba
cat > /tmp/test-syllabus.pdf << 'PDFEOF'
%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R/Resources<<>>>>endobj
4 0 obj<</Length 44>>stream
BT
/F1 12 Tf
100 700 Td
(Test Syllabus) Tj
ET
endstream
endobj
xref
0 5
0000000000 65535 f
0000000009 00000 n
0000000058 00000 n
0000000115 00000 n
0000000214 00000 n
trailer<</Size 5/Root 1 0 R>>
startxref
308
%%EOF
PDFEOF

# 7.2 - UPLOAD SÍLABO (EL FLUJO COMPLETO)
curl -X POST http://localhost:8080/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: multipart/form-data" \
  -F "courseId=1" \
  -F "action=create" \
  -F "file=@/tmp/test-syllabus.pdf" | jq '.'

# OUTPUT ESPERADO (201 CREATED):
# {
#   "syllabusId": 1,
#   "courseId": 1,
#   "version": 1,
#   "fileHash": "a1b2c3d4e5f6...",
#   "fileUrl": "http://127.0.0.1:9000/syllabi/course-1/...",
#   "fabricTxId": "tx-12345-uuid",  ←← IMPORTANTE: Transacción en Fabric
#   "status": "create",
#   "createdAt": "2026-04-08T10:00:00Z"
# }

# ✅ SI VES fabricTxId = ÉXITO COMPLETO

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 8: TESTING AUTOMATIZADO (OPCIONAL)                                 ║
╠════════════════════════════════════════════════════════════════════════════╣

# 8.1 - Hacer script ejecutable
chmod +x test_siladocs.sh

# 8.2 - Ejecutar suite completa de tests
./test_siladocs.sh

# 8.3 - Resultado: Todos los tests deberían pasar
#   1. Conectividad básica ✅
#   2. Health checks ✅
#   3. Autenticación ✅
#   4. Archivos de prueba ✅
#   5. Upload de documento ✅
#   6. Registro en Fabric ✅ (PRINCIPAL)
#   7. Verificación de BD ✅
#   8. Error handling ✅

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 9: TESTING CON POSTMAN (OPCIONAL - MÁS AMENO)                      ║
╠════════════════════════════════════════════════════════════════════════════╣

# 9.1 - Descargar Postman: https://www.postman.com/downloads/

# 9.2 - Importar colección:
#   Archivo > Import > SilaDocs_Fabric_Postman.json

# 9.3 - Configurar variables:
#   base_url: http://localhost:8080
#   fabric_url: http://127.0.0.1:8000
#   token: (obtenido del login)

# 9.4 - Ejecutar requests en orden:
#   1. Health Checks
#   2. Auth (Login)
#   3. Documents (Upload)
#   4. Syllabus (Upload) ← PRINCIPAL
#   5. Error Handling

╔════════════════════════════════════════════════════════════════════════════╗
║ PASO 10: INSPECCIONAR LOGS Y DATOS                                       ║
╠════════════════════════════════════════════════════════════════════════════╣

# 10.1 - Ver logs detallados de BlockchainService
# En la salida de la aplicación, busca:
# 🔄 INICIANDO UPLOAD DE SÍLABO
# 📝 Hash SHA-256 calculado
# ☁️ Archivo subido a MinIO
# ⛓️ REGISTRANDO EN HYPERLEDGER FABRIC
# ✅ FABRIC EXITOSO
# ✅ SÍLABO GUARDADO EN PostgreSQL

# 10.2 - Acceder a MinIO para ver arquivo
# Browser: http://localhost:9000
# Usuario: minioadmin / minioadmin
# Bucket: syllabi/course-1/...

# 10.3 - Consultar BD PostgreSQL
psql -h localhost -U siladocs_user -d siladocs

# En psql:
SELECT id, course_id, current_version, current_hash FROM syllabus;
SELECT id, file_url, status FROM syllabus;

# 10.4 - Ver transacciones en Fabric
# (Depende del middleware Python)
curl http://127.0.0.1:8000/transacciones/

EOF

echo ""
echo "================================================================================"
echo ""
echo "📊 FLUJO DE DATOS (Visualización)"
echo "================================================================================"
echo ""

cat << 'EOF'

┌─────────────────┐
│   Cliente REST  │  (Postman, Curl, etc.)
└────────┬────────┘
         │ POST /api/syllabi/upload (MultipartFile)
         ↓
┌────────────────────────────────┐
│  SillabusService.uploadSyllabus │  Flujo Estricto:
├────────────────────────────────┤  1. Validar
│ 1. Validar entrada             │  2. Calcular hash SHA-256
│ 2. Calcular SHA-256            │  3. Subir a MinIO
│ 3. Subir a MinIO               │  4. Registrar en Fabric
│ 4. Registrar en Fabric ⛓️      │  5. Guardar en PostgreSQL
│ 5. Guardar en PostgreSQL       │
└────────┬────────────────────────┘
         │
    ┌────┴────────────┬─────────────┐
    ↓                 ↓             ↓
┌─────────────┐  ┌──────────┐  ┌──────────────┐
│   MinIO     │  │ Fabric   │  │  PostgreSQL  │
│             │  │ Middleware       │
│ (S3-compat) │  │ (REST API)       │
│             │  │ Python FastAPI  │
└─────────────┘  └──────────┘  └──────────────┘

Resultado: fabricTxId devuelto al cliente

EOF

echo ""
echo "================================================================================"
echo ""
echo "⚡ CHECKLIST RÁPIDO"
echo "================================================================================"
echo ""

cat << 'EOF'
Pre-requisitos:
  ☐ Java 21 instalado
  ☐ Maven 3.8+ instalado
  ☐ Docker & Docker Compose
  ☐ Git (proyecto clonado)

Configuración:
  ☐ .env creado con variables
  ☐ application-fabric.yml verificado
  ☐ Middleware Fabric corriendo en puerto 8000

Compilación:
  ☐ mvn clean
  ☐ mvn compile -DskipTests
  ☐ mvn test (tests unitarios)
  ☐ mvn package -DskipTests (JAR creado)

Ejecución:
  ☐ PostgreSQL iniciado (docker-compose up -d postgresql)
  ☐ MinIO iniciado (docker-compose up -d minio)
  ☐ Fabric API ejecutándose (puerto 8000)
  ☐ SilaDocs Backend iniciado (java -jar ...)

Testing:
  ☐ GET /health (200 OK)
  ☐ GET /health/fabric (200 OK)
  ☐ POST /auth/login (obtener token)
  ☐ POST /api/syllabi/upload (flujo completo) ← PRINCIPAL
  ☐ Ver fabricTxId en respuesta
  ☐ Verificar archivo en MinIO
  ☐ Verificar registro en PostgreSQL

EOF

echo ""
echo "================================================================================"
echo ""
echo "📚 DOCUMENTACIÓN"
echo "================================================================================"
echo ""
echo "  📖 SETUP_FABRIC.md (Guía completa de 200+ líneas)"
echo "  📖 test_siladocs.sh (Script de testing automatizado)"
echo "  📖 SilaDocs_Fabric_Postman.json (Colección para Postman)"
echo ""
echo "  Para tests manuales:"
echo "    $ chmod +x test_siladocs.sh"
echo "    $ ./test_siladocs.sh"
echo ""
echo "================================================================================"
echo ""
echo "✅ TODO LISTO PARA PROBAR"
echo ""
echo "Próximos pasos:"
echo "  1. Seguir PASO A PASO arriba"
echo "  2. Ejecutar ./test_siladocs.sh para validar"
echo "  3. Ver SETUP_FABRIC.md para troubleshooting"
echo ""
echo "================================================================================"
echo ""
