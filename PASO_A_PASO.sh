#!/bin/bash

################################################################################
# SilaDocs Backend - Paso a Paso para Probar
# Tiempo total: ~15-20 minutos
################################################################################

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║  SilaDocs Backend - Testing Step by Step                         ║"
echo "║  Hyperledger Fabric Edition                                      ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# PASO 1: PREPARAR ENTORNO
# ============================================================================

echo "📋 PASO 1: Preparar Entorno"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "Ejecuta en PowerShell/Bash:"
echo ""
echo "  cd c:\Personal\Universidad\TP2\GitHub\siladocs-backend"
echo ""
echo "O si está en otra ubicación, cambia a la raíz del proyecto"
echo ""
read -p "Presiona ENTER cuando estés en la carpeta correcta..."
echo ""

# ============================================================================
# PASO 2: COMPILAR
# ============================================================================

echo ""
echo "🔨 PASO 2: Compilar el proyecto"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "Ejecuta:"
echo ""
echo "  mvn clean compile -DskipTests"
echo ""
echo "⏱️  Esto tomará ~1-2 minutos"
echo ""
read -p "Presiona ENTER cuando haya terminado (busca 'BUILD SUCCESS')..."
echo ""

# ============================================================================
# PASO 3: TESTS UNITARIOS (OPCIONAL)
# ============================================================================

echo ""
echo "🧪 PASO 3: Ejecutar Tests Unitarios (OPCIONAL)"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "Ejecuta:"
echo ""
echo "  mvn test"
echo ""
echo "⏱️  Esto tomará ~2-3 minutos"
echo ""
echo "Si ves 'Tests run: X, Failures: 0' = ✅ ÉXITO"
echo ""
read -p "Presiona ENTER cuando haya terminado (es opcional, puedes saltarlo)..."
echo ""

# ============================================================================
# PASO 4: CONSTRUIR JAR
# ============================================================================

echo ""
echo "📦 PASO 4: Construir JAR ejecutable"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "Ejecuta:"
echo ""
echo "  mvn package -DskipTests"
echo ""
echo "⏱️  Esto tomará ~2-3 minutos"
echo ""
read -p "Presiona ENTER cuando haya terminado (busca 'BUILD SUCCESS')..."
echo ""

# ============================================================================
# PASO 5: INICIAR SERVICIOS (NUEVA TERMINAL)
# ============================================================================

echo ""
echo "🐳 PASO 5: Iniciar Servicios de Soporte"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "⚠️  Abre UNA NUEVA TERMINAL (Ctrl+Shift+N en PowerShell)"
echo ""
echo "En la nueva terminal, ejecuta:"
echo ""
echo "  docker-compose up -d postgres minio"
echo ""
echo "Verifica con:"
echo ""
echo "  docker-compose ps"
echo ""
echo "Deberías ver postgres y minio con 'Up' status"
echo ""
read -p "Presiona ENTER cuando los servicios estén activos..."
echo ""

# ============================================================================
# PASO 6: MIDDLEWARE FABRIC (NUEVA TERMINAL)
# ============================================================================

echo ""
echo "⛓️  PASO 6: Iniciar Middleware Fabric"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "⚠️  Abre OTRA NUEVA TERMINAL"
echo ""
echo "Navega a la carpeta del middleware Fabric:"
echo ""
echo "  cd ../siladocs-fabric-middleware"
echo ""
echo "Ejecuta:"
echo ""
echo "  python -m uvicorn main:app --host 0.0.0.0 --port 8000"
echo ""
echo "Deberías ver:"
echo "  'Uvicorn running on http://0.0.0.0:8000'"
echo ""
read -p "Presiona ENTER cuando estés viendo ese mensaje (NO cierres la terminal)..."
echo ""

# ============================================================================
# PASO 7: BACKEND SILADOCS (TERCERA TERMINAL)
# ============================================================================

echo ""
echo "🚀 PASO 7: Ejecutar SilaDocs Backend"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "⚠️  Abre OTRA NUEVA TERMINAL (tercera)"
echo ""
echo "Navega al proyecto:"
echo ""
echo "  cd c:\Personal\Universidad\TP2\GitHub\siladocs-backend"
echo ""
echo "Ejecuta:"
echo ""
echo "  java -jar target/siladocs-backend.jar --spring.profiles.active=fabric"
echo ""
echo "Espera a ver:"
echo "  'Started SiladocsBackendApplication in X.XXX seconds'"
echo "  'Tomcat started on port(s): 8080'"
echo ""
read -p "Presiona ENTER cuando veas ese mensaje (NO cierres la terminal)..."
echo ""

# ============================================================================
# PASO 8: HEALTH CHECK
# ============================================================================

echo ""
echo "✅ PASO 8: Verificar Health Check"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "Abre OTRA NUEVA TERMINAL (cuarta - para testing)"
echo ""
echo "Ejecuta:"
echo ""
echo "  curl http://localhost:8080/health/fabric"
echo ""
echo "Deberías ver (200 OK):"
echo "  {"
echo "    \"timestamp\": \"2026-04-08T...\","
echo "    \"status\": \"UP\","
echo "    \"service\": \"Hyperledger Fabric\","
echo "    \"message\": \"API disponible\""
echo "  }"
echo ""
read -p "Presiona ENTER cuando veas la respuesta..."
echo ""

# ============================================================================
# PASO 9: LOGIN
# ============================================================================

echo ""
echo "🔐 PASO 9: Login y Obtener Token JWT"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "En la terminal de testing, ejecuta:"
echo ""
cat << 'EOF'
curl -X POST http://localhost:8080/auth/login `
  -H "Content-Type: application/json" `
  -d '{
    "email": "admin@siladocs.com",
    "password": "password123"
  }'
EOF
echo ""
echo ""
echo "Deberías recibir:"
echo "  {"
echo "    \"token\": \"eyJhbGciOiJIUzI1...\","
echo "    \"email\": \"admin@siladocs.com\","
echo "    \"expiresIn\": 86400000"
echo "  }"
echo ""
echo "⚠️  COPIA el valor del token (toda la cadena)"
echo ""
read -p "Presiona ENTER cuando hayas copiado el token..."
echo ""

# ============================================================================
# PASO 10: CREAR ARCHIVO DE PRUEBA
# ============================================================================

echo ""
echo "📄 PASO 10: Crear Archivo de Prueba"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "En PowerShell, ejecuta:"
echo ""
cat << 'PSEOF'
@'
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
'@ | Out-File -Encoding ASCII -Path C:\temp\test-syllabus.pdf
PSEOF
echo ""
echo "Si no existe C:\temp, crea la carpeta primero:"
echo "  mkdir C:\temp"
echo ""
read -p "Presiona ENTER cuando hayas creado el archivo..."
echo ""

# ============================================================================
# PASO 11: PRUEBA PRINCIPAL - UPLOAD A FABRIC
# ============================================================================

echo ""
echo "🎯 PASO 11: PRUEBA PRINCIPAL - Upload Sílabo a Fabric"
echo "════════════════════════════════════════════════════════════════════"
echo ""
echo "En PowerShell, ejecuta (reemplaza TOKEN con tu token):"
echo ""
cat << 'EOF'
$TOKEN = "eyJhbGciOiJIUzI1..."  # Reemplaza con tu token

curl -X POST http://localhost:8080/api/syllabi/upload `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: multipart/form-data" `
  -F "courseId=1" `
  -F "action=create" `
  -F "file=@C:\temp\test-syllabus.pdf" | ConvertFrom-Json | ConvertTo-Json
EOF
echo ""
echo ""
echo "✅ SI VES response 201 CREATED con 'fabricTxId': ÉXITO TOTAL"
echo ""
echo "Response esperado:"
echo "  {"
echo "    \"syllabusId\": 1,"
echo "    \"courseId\": 1,"
echo "    \"version\": 1,"
echo "    \"fileHash\": \"abc123...\","
echo "    \"fileUrl\": \"http://127.0.0.1:9000/syllabi/...\","
echo "    \"fabricTxId\": \"tx-12345-uuid\",  ← IMPORTANTE"
echo "    \"status\": \"create\""
echo "  }"
echo ""
read -p "Presiona ENTER cuando hayas ejecutado el comando de upload..."
echo ""

# ============================================================================
# PASO 12: VERIFICACIÓN FINAL
# ============================================================================

echo ""
echo "🔍 PASO 12: Verificación Final"
echo "────────────────────────────────────────────────────────────────────"
echo ""
echo "Verifica en la terminal de SilaDocs Backend (terminal 3) que ves logs como:"
echo ""
echo "  🔄 INICIANDO UPLOAD DE SÍLABO"
echo "  📝 Hash SHA-256 calculado"
echo "  ☁️  Archivo subido a MinIO"
echo "  ⛓️  REGISTRANDO EN HYPERLEDGER FABRIC"
echo "  ✅ FABRIC EXITOSO: txId=tx-..."
echo "  ✅ SÍLABO GUARDADO EN PostgreSQL"
echo ""
echo "Si ves TODO eso = ✅ REFACTORING COMPLETADO EXITOSAMENTE"
echo ""
read -p "Presiona ENTER cuando hayas verificado los logs..."
echo ""

# ============================================================================
# RESUMEN
# ============================================================================

echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                  ✅ TESTING COMPLETADO ✅                        ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""
echo "Lo que probaste:"
echo "  ✅ Compilación sin errores"
echo "  ✅ BD PostgreSQL activa"
echo "  ✅ MinIO activo (S3-compatible)"
echo "  ✅ Middleware Fabric activo"
echo "  ✅ SilaDocs Backend corriendo"
echo "  ✅ JWT authentication funcionando"
echo "  ✅ Upload a MinIO funcionando"
echo "  ✅ Registro en Hyperledger Fabric funcionando"
echo "  ✅ Persistencia en PostgreSQL funcionando"
echo ""
echo "Próximos pasos:"
echo "  • Para más testing: ejecutar './test_siladocs.sh'"
echo "  • Para Postman: importar 'SilaDocs_Fabric_Postman.json'"
echo "  • Para troubleshooting: ver 'SETUP_FABRIC.md'"
echo ""

