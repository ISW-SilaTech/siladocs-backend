# 🧪 Plan de Testing Completo - SilaDocs Backend + Blockchain

## 📋 Índice
1. [Tests del Backend (Sin Blockchain)](#tests-backend)
2. [Tests de Integración con Fabric](#tests-fabric)
3. [Tests End-to-End](#tests-e2e)
4. [Comandos Listos para Copiar/Pegar](#comandos)

---

## <a name="tests-backend"></a>1️⃣ TESTS DEL BACKEND (Sin Blockchain)

### A. Health Check
```bash
# Test 1: Health general del backend
curl -X GET https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/health

# Respuesta esperada (200 OK):
{
  "timestamp": "2026-04-25T10:30:00Z",
  "status": "UP",
  "application": "SilaDocs Backend",
  "fabric_available": true
}
```

### B. Autenticación (JWT)

```bash
# Test 2: Login
curl -X POST https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@siladocs.com",
    "password": "password123"
  }'

# Respuesta esperada (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "admin@siladocs.com",
  "expiresIn": 86400000
}

# Guardar para próximos tests:
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### C. Crear Carrera, Plan de Estudios y Curso

```bash
# Test 3: Crear una Carrera
curl -X POST https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/api/careers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ingeniería en Sistemas",
    "description": "Plan de estudios de Ingeniería",
    "duration": 5,
    "faculty": "Ingeniería"
  }'

# Respuesta esperada (201 CREATED):
{
  "id": 1,
  "name": "Ingeniería en Sistemas",
  "description": "Plan de estudios de Ingeniería",
  "duration": 5,
  "faculty": "Ingeniería"
}
# Guardar: export CAREER_ID=1

# Test 4: Crear Plan de Estudios
curl -X POST https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/api/curriculums \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Malla 2024",
    "careerId": 1,
    "status": "ACTIVE"
  }'

# Respuesta esperada (201 CREATED):
{
  "id": 1,
  "name": "Malla 2024",
  "careerId": 1,
  "status": "ACTIVE"
}
# Guardar: export CURRICULUM_ID=1

# Test 5: Crear Curso
curl -X POST https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SIS101",
    "name": "Introducción a Sistemas",
    "faculty": "Ingeniería",
    "curriculumId": 1,
    "year": 1,
    "status": "ACTIVE"
  }'

# Respuesta esperada (201 CREATED):
{
  "id": 1,
  "code": "SIS101",
  "name": "Introducción a Sistemas",
  "faculty": "Ingeniería",
  "curriculumId": 1,
  "year": 1,
  "status": "ACTIVE"
}
# Guardar: export COURSE_ID=1
```

---

## <a name="tests-fabric"></a>2️⃣ TESTS DE INTEGRACIÓN CON BLOCKCHAIN

### A. Verificar Fabric está disponible

```bash
# Test 6: Health check de Fabric desde el backend
curl -X GET https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/health/fabric \
  -H "Authorization: Bearer $TOKEN"

# Respuesta esperada (200 OK):
{
  "timestamp": "2026-04-25T10:35:25Z",
  "status": "UP",
  "service": "Hyperledger Fabric",
  "message": "API disponible"
}
```

### B. Subir Sílabo (EL TEST PRINCIPAL CON BLOCKCHAIN)

```bash
# Test 7: Crear un archivo de prueba
cat > /tmp/syllabus.pdf << 'EOF'
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>
endobj
xref
0 4
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
trailer
<< /Size 4 /Root 1 0 R >>
startxref
214
%%EOF
EOF

# Test 8: Subir Sílabo con Blockchain
curl -X POST https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "courseId=1" \
  -F "action=create" \
  -F "file=@/tmp/syllabus.pdf"

# Respuesta esperada (201 CREATED):
{
  "syllabusId": 1,
  "courseId": 1,
  "version": 1,
  "fileHash": "abc123def456...",                    # SHA-256 (64 hex chars)
  "fileUrl": "http://20.38.34.192:9000/syllabi/course-1/...",  # MinIO URL
  "fabricTxId": "tx-12345-uuid",                   # ⭐ BLOCKCHAIN TRANSACTION ID
  "status": "create",
  "createdAt": "2026-04-25T10:40:00Z"
}

# Verificar que fabricTxId no es null/empty ← BLOCKCHAIN FUNCIONANDO ✅
```

### C. Obtener Historial de Sílabo

```bash
# Test 9: Ver historial de versiones
curl -X GET https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/api/syllabi/1/history \
  -H "Authorization: Bearer $TOKEN"

# Respuesta esperada (200 OK):
{
  "syllabusId": 1,
  "history": [
    {
      "version": 1,
      "fileHash": "abc123def456...",
      "fabricTxId": "tx-12345-uuid",
      "status": "create",
      "createdAt": "2026-04-25T10:40:00Z"
    }
  ]
}
```

### D. Actualizar Sílabo (Verificar que crea nueva versión)

```bash
# Test 10: Actualizar sílabo con nuevo archivo
curl > /tmp/syllabus-v2.pdf << 'EOF'
%PDF-1.4
... (contenido diferente)
EOF

curl -X POST https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "courseId=1" \
  -F "action=update" \
  -F "file=@/tmp/syllabus-v2.pdf"

# Respuesta esperada (201 CREATED):
{
  "syllabusId": 1,
  "courseId": 1,
  "version": 2,              # ← Incrementó versión
  "fileHash": "def789ghi012...",  # ← Hash diferente
  "fabricTxId": "tx-67890-uuid",  # ← Nuevo txId en blockchain
  "status": "update",
  "createdAt": "2026-04-25T10:45:00Z"
}
```

---

## <a name="tests-e2e"></a>3️⃣ TESTS END-TO-END (Completo)

### Escenario: Registrar una carrera con sílabo en blockchain

```bash
#!/bin/bash

echo "=== SILADOCS E2E TEST SUITE ==="
echo ""

# Variables
BACKEND_URL="https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net"
FABRIC_URL="http://20.38.34.192:8000"

# ===== PASO 1: Health Checks =====
echo "1️⃣ Health Checks..."
curl -s $BACKEND_URL/health | jq '.status' | grep -q "UP" && echo "✅ Backend UP" || echo "❌ Backend DOWN"
curl -s $FABRIC_URL/health | jq '.status' | grep -q "healthy" && echo "✅ Fabric healthy" || echo "❌ Fabric DOWN"
echo ""

# ===== PASO 2: Autenticación =====
echo "2️⃣ Autenticación..."
LOGIN_RESPONSE=$(curl -s -X POST $BACKEND_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@siladocs.com",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
echo "✅ Token obtenido: ${TOKEN:0:20}..."
echo ""

# ===== PASO 3: Crear Estructura =====
echo "3️⃣ Crear Carrera, Plan y Curso..."

CAREER=$(curl -s -X POST $BACKEND_URL/api/careers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ing. Sistemas","faculty":"Ingeniería"}')
CAREER_ID=$(echo $CAREER | jq -r '.id')
echo "✅ Carrera creada: ID=$CAREER_ID"

CURRICULUM=$(curl -s -X POST $BACKEND_URL/api/curriculums \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Malla 2024\",\"careerId\":$CAREER_ID}")
CURRICULUM_ID=$(echo $CURRICULUM | jq -r '.id')
echo "✅ Plan creado: ID=$CURRICULUM_ID"

COURSE=$(curl -s -X POST $BACKEND_URL/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"SIS101\",\"name\":\"Intro a Sistemas\",\"curriculumId\":$CURRICULUM_ID,\"year\":1}")
COURSE_ID=$(echo $COURSE | jq -r '.id')
echo "✅ Curso creado: ID=$COURSE_ID"
echo ""

# ===== PASO 4: Subir Sílabo a Blockchain =====
echo "4️⃣ Subir Sílabo a Blockchain..."

# Crear archivo de prueba
cat > /tmp/test-syllabus.pdf << 'PDFEOF'
%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj
xref
0 4
0000000000 65535 f
0000000009 00000 n
0000000058 00000 n
0000000115 00000 n
trailer<</Size 4/Root 1 0 R>>
startxref
214
%%EOF
PDFEOF

# Subir
SYLLABUS=$(curl -s -X POST $BACKEND_URL/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "courseId=$COURSE_ID" \
  -F "action=create" \
  -F "file=@/tmp/test-syllabus.pdf")

SYLLABUS_ID=$(echo $SYLLABUS | jq -r '.syllabusId')
FILE_HASH=$(echo $SYLLABUS | jq -r '.fileHash')
FABRIC_TX=$(echo $SYLLABUS | jq -r '.fabricTxId')

echo "✅ Sílabo subido:"
echo "   - ID: $SYLLABUS_ID"
echo "   - Hash: ${FILE_HASH:0:16}..."
echo "   - Fabric TX: $FABRIC_TX"

# Verificar que está en blockchain
if [ "$FABRIC_TX" != "null" ] && [ ! -z "$FABRIC_TX" ]; then
    echo "✅ ⭐ BLOCKCHAIN TRANSACTION EXITOSA"
else
    echo "❌ ⭐ BLOCKCHAIN FALLÓ - No hay txId"
    exit 1
fi
echo ""

# ===== PASO 5: Verificar en Blockchain =====
echo "5️⃣ Verificar documento en Fabric Middleware..."
curl -s -X GET "$FABRIC_URL/leer-documento/$SYLLABUS_ID" | jq .
echo ""

# ===== PASO 6: Historial =====
echo "6️⃣ Ver historial de versiones..."
curl -s -X GET $BACKEND_URL/api/syllabi/$SYLLABUS_ID/history \
  -H "Authorization: Bearer $TOKEN" | jq .
echo ""

echo "🎉 TEST SUITE COMPLETADO EXITOSAMENTE"
```

---

## <a name="comandos"></a>4️⃣ COMANDOS LISTOS PARA COPIAR/PEGAR

### PowerShell
```powershell
# Variables
$BACKEND = "https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net"
$FABRIC = "http://20.38.34.192:8000"

# Test 1: Health
curl $BACKEND/health -UseBasicParsing | Select-Object -ExpandProperty Content | ConvertFrom-Json | Select-Object status

# Test 2: Fabric Health
curl $BACKEND/health/fabric -Headers @{"Authorization"="Bearer YOUR_TOKEN"} -UseBasicParsing | ConvertFrom-Json

# Test 3: Login
$loginResponse = curl -X POST $BACKEND/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@siladocs.com","password":"password123"}' `
  -UseBasicParsing | ConvertFrom-Json

$TOKEN = $loginResponse.token
echo "Token: $TOKEN"

# Test 4: Crear Carrera
curl -X POST "$BACKEND/api/careers" `
  -Headers @{"Authorization"="Bearer $TOKEN"; "Content-Type"="application/json"} `
  -Body '{"name":"Ingeniería Sistemas","faculty":"Ingeniería"}' `
  -UseBasicParsing | ConvertFrom-Json
```

### Bash/Linux
```bash
# Variables
BACKEND="https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net"
FABRIC="http://20.38.34.192:8000"

# Test 1: Health
curl -s $BACKEND/health | jq '.status'

# Test 2: Fabric Health  
curl -s -H "Authorization: Bearer $TOKEN" $BACKEND/health/fabric | jq '.status'

# Test 3: Login
TOKEN=$(curl -s -X POST $BACKEND/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@siladocs.com","password":"password123"}' | jq -r '.token')
echo "Token: $TOKEN"

# Test 4: Crear Carrera
curl -s -X POST "$BACKEND/api/careers" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ing. Sistemas","faculty":"Ingeniería"}' | jq .
```

---

## 📊 MATRIZ DE TESTING

| Test | Endpoint | Autenticación | ¿Usa Blockchain? | Respuesta Esperada |
|------|----------|----------------|-------------------|-------------------|
| 1 | GET /health | ❌ | ❌ | 200 OK, status=UP |
| 2 | GET /health/fabric | ✅ | ❌ | 200 OK, service=Fabric |
| 3 | POST /auth/login | ❌ | ❌ | 200 OK, token JWT |
| 4 | POST /api/careers | ✅ | ❌ | 201 CREATED |
| 5 | POST /api/curriculums | ✅ | ❌ | 201 CREATED |
| 6 | POST /api/courses | ✅ | ❌ | 201 CREATED |
| 7 | POST /api/syllabi/upload | ✅ | ✅ | 201 CREATED + fabricTxId |
| 8 | GET /api/syllabi/{id}/history | ✅ | ❌ | 200 OK + versiones |

---

## 🔍 QUÉ VERIFICAR EN CADA TEST

### Test de Sílabo (El más importante)
```json
{
  "syllabusId": <número>,           // ✅ Debe existir
  "courseId": <número>,              // ✅ Debe coincidir
  "version": <número>,               // ✅ Debe ser 1 en crear
  "fileHash": "<64 hex chars>",       // ✅ SHA-256 válido
  "fileUrl": "http://...",            // ✅ URL MinIO válida
  "fabricTxId": "tx-...",            // ✅ ⭐ MUY IMPORTANTE: NO debe ser null
  "status": "create",                 // ✅ Debe ser "create"/"update"
  "createdAt": "2026-..."             // ✅ Timestamp válido
}
```

**Si `fabricTxId` es null:** Blockchain no funcionó. Verificar:
1. Fabric Middleware corriendo: `curl http://20.38.34.192:8000/health`
2. FABRIC_API_URL correcta en Web App
3. Logs de Web App: Azure Portal → Log stream

---

## 🚨 SOLUCIÓN DE PROBLEMAS

### Error: "Fabric API no disponible"
```bash
# Verificar conectividad
curl http://20.38.34.192:8000/health

# Si falla, conectar a VM y verificar
ssh -i ~/.ssh/fabric-vm-key2.pem azureuser@20.38.34.192
docker ps | grep fabric-middleware
docker logs fabric-middleware
```

### Error: 403 Forbidden
- Probablemente autenticación JWT requerida
- Usar: `-H "Authorization: Bearer $TOKEN"`

### Error: Timeout
- Aumentar FABRIC_READ_TIMEOUT en Web App (60000 ms)
- Verificar que Fabric Middleware no está lento

---

**Próximo paso:** Ejecuta los tests en orden y reporta qué funciona y qué no 🚀
