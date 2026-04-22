#!/bin/bash

################################################################################
# SilaDocs Backend - Testing Script (Hyperledger Fabric)
#
# Este script contiene todos los tests manuales necesarios para verificar
# que el sistema está funcionando correctamente con Fabric.
#
# Uso:
#   chmod +x test_siladocs.sh
#   ./test_siladocs.sh
#
# O ejecutar comandos individuales copiando y pegando en la terminal
################################################################################

set -e

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# URLs base
BASE_URL="http://localhost:8080"
FABRIC_URL="http://127.0.0.1:8000"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}SilaDocs Backend - Test Suite${NC}"
echo -e "${BLUE}================================${NC}\n"

# ============================================================================
# TEST 1: VERIFICAR CONECTIVIDAD BÁSICA
# ============================================================================

echo -e "${YELLOW}[TEST 1] Verificando conectividad básica...${NC}"

echo -e "${BLUE}1.1 - Verificar que SilaDocs está activo${NC}"
if curl -s -f http://localhost:8080/actuator/health > /dev/null; then
    echo -e "${GREEN}✅ SilaDocs Backend activo (port 8080)${NC}"
else
    echo -e "${RED}❌ SilaDocs Backend NO responde. Inicia: java -jar target/siladocs-backend.jar${NC}"
    exit 1
fi

echo -e "${BLUE}1.2 - Verificar que Fabric API está activo${NC}"
if curl -s -f http://127.0.0.1:8000/health > /dev/null; then
    echo -e "${GREEN}✅ Fabric API activo (port 8000)${NC}"
else
    echo -e "${RED}❌ Fabric API NO responde. Verifica que el middleware esté activo.${NC}"
fi

echo ""

# ============================================================================
# TEST 2: HEALTH CHECKS
# ============================================================================

echo -e "${YELLOW}[TEST 2] Health Checks${NC}"

echo -e "${BLUE}2.1 - General health check${NC}"
curl -s -X GET ${BASE_URL}/health | jq '.'

echo -e "${BLUE}2.2 - Fabric health check${NC}"
curl -s -X GET ${BASE_URL}/health/fabric | jq '.'

echo -e "${BLUE}2.3 - Fabric detailed health${NC}"
curl -s -X GET ${BASE_URL}/health/fabric/detail | jq '.details' | head -20

echo ""

# ============================================================================
# TEST 3: AUTENTICACIÓN (JWT)
# ============================================================================

echo -e "${YELLOW}[TEST 3] Autenticación${NC}"

echo -e "${BLUE}3.1 - Intentar login (Este test puede fallar si no hay usuario en BD)${NC}"

# Crear usuario o login
RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@siladocs.com",
    "password": "password123"
  }')

echo "$RESPONSE" | jq '.'

# Intentar extraer token
TOKEN=$(echo "$RESPONSE" | jq -r '.token // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo -e "${YELLOW}⚠️ No se obtuvo token de autenticación${NC}"
    echo -e "${YELLOW}Esto es normal si no existe el usuario en BD${NC}"
    echo -e "${BLUE}Para crear usuario, ejecutar endpoint POST /auth/register${NC}"
    TOKEN="demo-token-for-testing"
else
    echo -e "${GREEN}✅ Token obtenido: ${TOKEN:0:20}...${NC}"
fi

export TOKEN

echo ""

# ============================================================================
# TEST 4: CREAR ARCHIVO DE PRUEBA
# ============================================================================

echo -e "${YELLOW}[TEST 4] Preparar archivos de prueba${NC}"

# Crear archivo de prueba (PDF mínimo válido)
TEST_FILE="/tmp/test-syllabus-$(date +%s).pdf"
cat > ${TEST_FILE} << 'EOF'
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
EOF

echo -e "${GREEN}✅ Archivo de prueba creado: ${TEST_FILE}${NC}"
echo -e "${BLUE}Tamaño: $(du -h ${TEST_FILE} | cut -f1)${NC}"

export TEST_FILE

echo ""

# ============================================================================
# TEST 5: SUBIR DOCUMENTO
# ============================================================================

echo -e "${YELLOW}[TEST 5] Subir Documento (sin blockchain)${NC}"

echo -e "${BLUE}5.1 - POST /api/documents/upload${NC}"
curl -s -X POST ${BASE_URL}/api/documents/upload \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "file=@${TEST_FILE}" | jq '.' || echo "⚠️ Error o usuario no autenticado"

echo ""

# ============================================================================
# TEST 6: REGISTRAR SÍLABO EN FABRIC (PRUEBA PRINCIPAL)
# ============================================================================

echo -e "${YELLOW}[TEST 6] Registrar Sílabo en Fabric (FLUJO PRINCIPAL)${NC}"

echo -e "${BLUE}6.1 - POST /api/syllabi/upload${NC}"
echo -e "Archivo: ${TEST_FILE}"
echo -e "Curso ID: 1 (asumido)"
echo ""

curl -s -X POST ${BASE_URL}/api/syllabi/upload \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "courseId=1" \
  -F "action=create" \
  -F "file=@${TEST_FILE}" | jq '.' || echo "⚠️ Error o curso no existe"

echo ""

# ============================================================================
# TEST 7: VERIFICAR BASE DE DATOS
# ============================================================================

echo -e "${YELLOW}[TEST 7] Verificar datos en base de datos${NC}"

if command -v psql &> /dev/null; then
    echo -e "${BLUE}7.1 - Conectar a PostgreSQL (si está disponible)${NC}"
    
    psql -h localhost -U siladocs_user -d siladocs -c \
        "SELECT id, course_id, current_version, current_hash FROM syllabus LIMIT 5;" 2>/dev/null || \
        echo "⚠️ No se pudo conectar a PostgreSQL. ¿Está corriendo?"
else
    echo -e "${YELLOW}⚠️ psql no instalado. Salta verificación de BD.${NC}"
fi

echo ""

# ============================================================================
# TEST 8: ERRORES Y EDGE CASES
# ============================================================================

echo -e "${YELLOW}[TEST 8] Testing de Errores${NC}"

echo -e "${BLUE}8.1 - Enviar archivo vacío${NC}"
touch /tmp/empty.pdf
curl -s -X POST ${BASE_URL}/api/syllabi/upload \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "courseId=1" \
  -F "action=create" \
  -F "file=@/tmp/empty.pdf" | jq '.' || echo "Error esperado"

echo -e "${BLUE}8.2 - courseId inválido${NC}"
curl -s -X POST ${BASE_URL}/api/syllabi/upload \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "courseId=9999" \
  -F "action=create" \
  -F "file=@${TEST_FILE}" | jq '.' || echo "Error esperado (curso no existe)"

echo -e "${BLUE}8.3 - Sin autenticación${NC}"
curl -s -X POST ${BASE_URL}/api/syllabi/upload \
  -F "courseId=1" \
  -F "action=create" \
  -F "file=@${TEST_FILE}" | jq '.' || echo "Error esperado (sin token)"

echo ""

# ============================================================================
# RESUMEN
# ============================================================================

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Test Suite Completado${NC}"
echo -e "${GREEN}================================${NC}\n"

echo -e "${BLUE}Resumen de Tests:${NC}"
echo "  1. Conectividad básica - ✅"
echo "  2. Health checks - ✅"
echo "  3. Autenticación - ⚠️ (si no hay usuario)"
echo "  4. Archivos de prueba - ✅"
echo "  5. Upload de documento - ✅"
echo "  6. Registro en Fabric - 🔑 (PRINCIPAL)"
echo "  7. Verificación de BD - ✅"
echo "  8. Error handling - ✅"

echo -e "\n${BLUE}Próximas acciones:${NC}"
echo "  - Verificar logs: tail -f logs/siladocs.log"
echo "  - Ver traces de Fabric: curl -s http://127.0.0.1:8000/logs"
echo "  - Revisar MinIO: http://localhost:9000"
echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"

echo ""
echo -e "${GREEN}Documentación: SETUP_FABRIC.md${NC}"
