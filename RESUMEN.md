#!/bin/bash

###############################################################################
#
#  ██████╗ ███████╗███████╗██╗   ██╗███╗   ███╗███████╗███╗   ██╗
#  ██╔════╝ ██╔════╝██╔════╝██║   ██║████╗ ████║██╔════╝████╗  ██║
#  ██║  ███╗█████╗  █████╗  ██║   ██║██╔████╔██║█████╗  ██╔██╗ ██║
#  ██║   ██║██╔══╝  ██╔══╝  ██║   ██║██║╚██╔╝██║██╔══╝  ██║╚██╗██║
#  ╚██████╔╝███████╗██║     ╚██████╔╝██║ ╚═╝ ██║███████╗██║ ╚████║
#   ╚═════╝ ╚══════╝╚═╝      ╚═════╝ ╚═╝     ╚═╝╚══════╝╚═╝  ╚═══╝
#
#  SilaDocs Backend Refactoring - COMPLETADO ✅
#  Ethereum (Web3j/Ganache) → Hyperledger Fabric (REST API)
#
###############################################################################

clear

echo ""
echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║                                                                          ║"
echo "║              SilaDocs Backend Refactoring - COMPLETADO ✅                ║"
echo "║                                                                          ║"
echo "║              Migración: Ethereum → Hyperledger Fabric                    ║"
echo "║                                                                          ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}📊 RESUMEN EJECUTIVO${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${GREEN}✅ ARCHIVOS CREADOS: 13${NC}"
echo "   • BlockchainException.java"
echo "   • BlockchainFabricRequestDto.java"
echo "   • BlockchainFabricResponseDto.java"
echo "   • StorageService.java (MinIO integration)"
echo "   • HealthController.java (health checks)"
echo "   • BlockchainServiceTest.java (unit tests)"
echo "   • application-fabric.yml"
echo "   • SETUP_FABRIC.md (guía 200+ líneas)"
echo "   • TESTING_GUIDE.sh (paso a paso)"
echo "   • test_siladocs.sh (tests automatizados)"
echo "   • SilaDocs_Fabric_Postman.json (colección)"
echo "   • README_FABRIC.md (quick start)"
echo "   • RESUMEN.md (este archivo)"
echo ""

echo -e "${GREEN}✅ ARCHIVOS REFACTORIZADOS: 4${NC}"
echo "   • BlockchainService.java (Web3j → RestClient)"
echo "   • SyllabusService.java (nuevo flujo: Fabric → BD)"
echo "   • BlockchainConfig.java (RestClient en lugar de Web3j)"
echo "   • pom.xml (Web3j removido)"
echo ""

echo -e "${RED}❌ DEPENDENCIAS REMOVIDAS: 1${NC}"
echo "   • org.web3j:core:4.10.0"
echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}🔄 CAMBIOS ARQUITECTÓNICOS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${YELLOW}ANTES (Ethereum/Ganache):${NC}"
cat << 'EOF'
┌──────────────────┐
│  Spring Boot     │
│   BlockchainSvc  │ ← Web3j library
│                  │
└────────┬─────────┘
         │ RLP-encoded transactions
         ↓
    [Ganache]
    Local Ethereum
    Smart Contracts (Solidity)
    ECDSA signatures
EOF

echo ""
echo -e "${YELLOW}DESPUÉS (Hyperledger Fabric):${NC}"
cat << 'EOF'
┌──────────────────────┐
│   Spring Boot        │
│ BlockchainService    │ ← RestClient (HTTP)
│ (RestClient 3.x)     │
└─────────┬────────────┘
          │ POST JSON
          ↓
  [Fabric Middleware]
  Python FastAPI
  Hyperledger Fabric
  Ledger-based consensus
EOF

echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}🎯 EL FLUJO PRINCIPAL (SyllabusService.uploadSyllabus)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo ""

cat << 'EOF'
┌─────────────────────────────────────┐
│ Cliente: POST /api/syllabi/upload   │ (multipart: file, courseId, action)
└──────────────┬──────────────────────┘
               ↓
┌──────────────────────────────────────────┐
│ 1. VALIDAR entrada                       │ (courseId > 0, archivo no vacío)
└──────────────┬───────────────────────────┘
               ↓
┌──────────────────────────────────────────┐
│ 2. CALCULAR SHA-256                      │ (DigestUtils.sha256Hex)
│    = f1e2d3c4...b5a6 (hash de auditoría) │
└──────────────┬───────────────────────────┘
               ↓
┌──────────────────────────────────────────┐
│ 3. SUBIR A MinIO (StorageService)        │ (S3-compatible)
│    URL = http://minio:9000/syllabi/...   │
└──────────────┬───────────────────────────┘
               ↓
┌──────────────────────────────────────────┐
│ 4. REGISTRAR EN FABRIC ⛓️ (CRÍTICO)      │ BlockchainService.register...InFabric
│    POST /registrar-hash                  │
│    {                                     │
│      "curso_id": "1",                    │
│      "file_hash": "f1e2d3c4...",        │
│      "issuer": "admin@siladocs.com",    │
│      "date": "2026-04-08"               │
│    }                                     │
│                                          │
│    Response:                             │
│    {                                     │
│      "status": "success",                │
│      "txId": "tx-12345-uuid" ← IMPORTANTE │
│    }                                     │
└──────────────┬───────────────────────────┘
               │
               ├─→ Si falla: BlockchainException
               │   Rollback TODO (transacción revierte)
               │
               ↓ (Si éxito)
┌──────────────────────────────────────────┐
│ 5. GUARDAR EN PostgreSQL                 │ (@Transactional, solo si Fabric OK)
│    INSERT INTO syllabus (...)            │
└──────────────┬───────────────────────────┘
               ↓
┌──────────────────────────────────────────┐
│ Cliente: 201 CREATED                     │
│ Response:                                │
│ {                                        │
│   "syllabusId": 1,                       │
│   "version": 1,                          │
│   "fileHash": "f1e2d3c4...",            │
│   "fileUrl": "http://minio:9000/...",   │
│   "fabricTxId": "tx-12345-uuid" ⭐      │
│   "status": "create"                     │
│ }                                        │
└──────────────────────────────────────────┘

✅ = Flujo completado exitosamente
⛓️  = Blockchain involvement (immutable)
⭐ = Prueba de registro en Blockchain
EOF

echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}🚀 PASO A PASO PARA PROBAR (15 minutos)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${YELLOW}PASO 1: Preparar Entorno (2 min)${NC}"
cat << 'EOF'
  $ cd ~/proyectos/siladocs-backend
  $ cat > .env << 'ENVFILE'
  FABRIC_API_URL=http://127.0.0.1:8000
  JWT_SECRET=tu_secret_seguro_2026
  MINIO_ENDPOINT=http://127.0.0.1:9000
  POSTGRES_USER=siladocs_user
  POSTGRES_PASSWORD=siladocs_password
  ENVFILE
EOF
echo ""

echo -e "${YELLOW}PASO 2: Compilar (3 min)${NC}"
cat << 'EOF'
  $ mvn clean compile -DskipTests
  $ mvn test                          # Tests unitarios
  $ mvn package -DskipTests           # JAR final
EOF
echo ""

echo -e "${YELLOW}PASO 3: Iniciar Servicios (2 min)${NC}"
cat << 'EOF'
  Terminal 1:
  $ docker-compose up -d postgresql minio
  $ sleep 10

  Terminal 2 (desde siladocs-fabric-middleware):
  $ python -m uvicorn main:app --host 0.0.0.0 --port 8000
  
  Terminal 3 (SilaDocs):
  $ java -jar target/siladocs-backend.jar --spring.profiles.active=fabric
EOF
echo ""

echo -e "${YELLOW}PASO 4: Testing Health Checks (1 min)${NC}"
cat << 'EOF'
  $ curl http://localhost:8080/health/fabric
  
  Expected (200 OK):
  {
    "status": "UP",
    "service": "Hyperledger Fabric",
    "message": "API disponible"
  }
EOF
echo ""

echo -e "${YELLOW}PASO 5: Obtener JWT Token (1 min)${NC}"
cat << 'EOF'
  $ curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@siladocs.com","password":"password123"}'
    
  Copy token response and export:
  $ export TOKEN="eyJhbGciOiJIUzI..."
EOF
echo ""

echo -e "${YELLOW}PASO 6: Crear Archivo de Prueba (1 min)${NC}"
cat << 'EOF'
  $ cat > /tmp/test-syllabus.pdf << 'PDFEOF'
  %PDF-1.4
  1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
  ...
  PDFEOF
EOF
echo ""

echo -e "${YELLOW}PASO 7: PRUEBA PRINCIPAL - Upload a Fabric (2 min)${NC}"
cat << 'EOF'
  $ curl -X POST http://localhost:8080/api/syllabi/upload \
    -H "Authorization: Bearer $TOKEN" \
    -F "courseId=1" \
    -F "action=create" \
    -F "file=@/tmp/test-syllabus.pdf" | jq '.'
  
  ✅ SUCCESS: Deberías ver "fabricTxId" en la respuesta
EOF
echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}📚 DOCUMENTACIÓN${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo ""

cat << 'EOF'
Archivo                          | Propósito
─────────────────────────────────┼──────────────────────────────────
SETUP_FABRIC.md                  | Guía completa (200+ líneas) 
TESTING_GUIDE.sh                 | Paso a paso ejecutable
test_siladocs.sh                 | Tests automatizados
SilaDocs_Fabric_Postman.json     | Importar en Postman
README_FABRIC.md                 | Quick start
RESUMEN.md (tú estás aquí)       | Overview visual

Para ejecutar tests:
  $ chmod +x test_siladocs.sh
  $ ./test_siladocs.sh
  
Para guía interactiva:
  $ bash TESTING_GUIDE.sh
EOF
echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}✨ MEJORAS IMPLEMENTADAS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo ""

cat << 'EOF'
SEGURIDAD:
  ✅ Validación robusta de entrada en BlockchainService
  ✅ BlockchainException personalizada con status codes
  ✅ Rollback automático (@Transactional) si falla Fabric
  ✅ Logging detallado con emojis para debugging

FUNCIONALIDAD:
  ✅ Flujo estricto garantizado: Fabric → BD→
  ✅ Health checks para Fabric y SilaDocs
  ✅ StorageService para MinIO (S3-compatible)
  ✅ Manejo de errores: 4xx, 5xx, timeout, conexión

TESTING:
  ✅ Unit tests con JUnit 5 + Mockito
  ✅ Script automatizado de testing (bash)
  ✅ Colección Postman lista para usar
  ✅ Validación de todos los casos de error

DOCUMENTACIÓN:
  ✅ Guía de setup 200+ líneas
  ✅ Troubleshooting completo
  ✅ Postman collection importable
  ✅ Scripts de ejemplo (curl)
EOF
echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}🎯 CHECKLIST FINAL${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
echo ""

cat << 'EOF'
Pre-requisitos:
  ☑ Java 21 instalado
  ☑ Maven 3.8+ instalado
  ☑ Docker & Docker Compose
  ☑ Git (proyecto clonado)

Configuración:
  ☑ .env creado
  ☑ application-fabric.yml verificado

Compilación:
  ☑ mvn clean
  ☑ mvn compile -DskipTests
  ☑ mvn test ✅
  ☑ mvn package -DskipTests ✅

Ejecución:
  ☑ Docker services (PostgreSQL, MinIO)
  ☑ Fabric middleware (puerto 8000)
  ☑ SilaDocs Backend (puerto 8080)

Testing:
  ☑ GET /health/fabric (200 OK)
  ☑ POST /auth/login (obtener token)
  ☑ POST /api/syllabi/upload (flujo completo)
  ☑ Ver fabricTxId en respuesta ⭐
EOF
echo ""

echo -e "${GREEN}╔══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                                                                          ║${NC}"
echo -e "${GREEN}║                   ✅ TODO LISTO PARA PROBAR ✅                          ║${NC}"
echo -e "${GREEN}║                                                                          ║${NC}"
echo -e "${GREEN}║  Próximos pasos:                                                         ║${NC}"
echo -e "${GREEN}║  1. Seguir PASO A PASO (15 minutos)                                     ║${NC}"
echo -e "${GREEN}║  2. Ejecutar ./test_siladocs.sh para validar                            ║${NC}"
echo -e "${GREEN}║  3. Ver SETUP_FABRIC.md si hay problemas                                ║${NC}"
echo -e "${GREEN}║  4. Importar Postman collection para testing ameno                      ║${NC}"
echo -e "${GREEN}║                                                                          ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

exit 0
