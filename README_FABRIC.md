# 🚀 SilaDocs Backend - Hyperledger Fabric Edition

**Versión:** 1.0-Fabric  
**Estado:** ✅ Refactorizado (Ethereum → Hyperledger Fabric)  
**Fecha:** Abril 2026

> Gestión de sílabos académicos con auditoría inmutable en Hyperledger Fabric

---

## 📋 Quick Start (5 minutos)

### Requisitos
```bash
✓ Java 21
✓ Maven 3.8+
✓ Docker & Docker Compose
✓ Port 8080, 5432, 9000, 8000 disponibles
```

### Ejecutar
```bash
# Terminal 1: Servicios de soporte
docker-compose up -d postgresql minio

# Terminal 2: Middleware Fabric (desde siladocs-fabric-middleware)
python -m uvicorn main:app --host 0.0.0.0 --port 8000

# Terminal 3: Compilar y ejecutar
mvn clean compile test
java -jar target/siladocs-backend.jar --spring.profiles.active=fabric
```

### Probar
```bash
# Health check
curl http://localhost:8080/health/fabric

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@siladocs.com","password":"password123"}'

# Upload Sílabo (FLUJO PRINCIPAL)
curl -X POST http://localhost:8080/api/syllabi/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "courseId=1" \
  -F "action=create" \
  -F "file=@/tmp/test.pdf"
```

---

## 📚 Documentación Completa

| Documento | Descripción |
|-----------|------------|
| **SETUP_FABRIC.md** | Guía full (200+ líneas) con troubleshooting |
| **TESTING_GUIDE.sh** | Paso a paso interactivo para testing |
| **test_siladocs.sh** | Suite automatizada de tests |
| **SilaDocs_Fabric_Postman.json** | Colección Postman importable |

---

## 🔄 Cambios Principales (Ethereum → Fabric)

### Arquitectura Anterior
```
Backend (Java) ← Web3j → Ganache (Ethereum)
         ↓ Smart Contracts (Solidity)
```

### Nueva Arquitectura
```
Backend (Java) ← RestClient → Fabric Middleware (Python FastAPI)
         ↓ HTTP POST JSON
```

### Archivos Modificados

**Creados (13):**
- ✨ `BlockchainException.java` - Custom exception
- ✨ `BlockchainFabricRequestDto.java` - Payload JSON
- ✨ `BlockchainFabricResponseDto.java` - Response JSON
- ✨ `HealthController.java` - Health checks
- ✨ `StorageService.java` - MinIO integration
- ✨ `BlockchainServiceTest.java` - Unit tests
- ✨ `application-fabric.yml` - Config
- ✨ + 6 documentos de guía/testing

**Refactorizados (4):**
- 🔄 `BlockchainService.java` - Migrado a RestClient
- 🔄 `SyllabusService.java` - Nuevo flujo Fabric→BD
- 🔄 `BlockchainConfig.java` - RestClient en lugar de Web3j
- 🔄 `pom.xml` - Web3j removido

---

## 🎯 El Flujo Principal (SillabusService)

```
┌──────────────────────────────────────────────────────┐
│ Cliente envía: POST /api/syllabi/upload              │
│ Payload: courseId, action, file                      │
└────────────┬─────────────────────────────────────────┘
             ↓
┌──────────────────────────────────────────────────────┐
│ 1. VALIDAR entrada                                   │
│    - courseId válido, archivo no vacío               │
└────────────┬─────────────────────────────────────────┘
             ↓
┌──────────────────────────────────────────────────────┐
│ 2. CALCULAR SHA-256 del archivo                      │
│    - Hash para auditoría blockchain                  │
└────────────┬─────────────────────────────────────────┘
             ↓
┌──────────────────────────────────────────────────────┐
│ 3. SUBIR A MinIO (StorageService)                    │
│    - S3-compatible storage                           │
│    - Retorna URL pública del archivo                 │
└────────────┬─────────────────────────────────────────┘
             ↓
┌──────────────────────────────────────────────────────┐
│ 4. REGISTRAR EN FABRIC (⛓️ CRÍTICO)                   │
│    - BlockchainService.registerSyllabusInFabric()    │
│    - POST JSON a Middleware Python (puerto 8000)     │
│    - Si falla → Excepción (rollback TODO)            │
└────────────┬─────────────────────────────────────────┘
             ↓
┌──────────────────────────────────────────────────────┐
│ 5. GUARDAR EN PostgreSQL                             │
│    - Solo si Fabric exitoso                          │
│    - @Transactional revierte si falla aquí           │
└────────────┬─────────────────────────────────────────┘
             ↓
┌──────────────────────────────────────────────────────┐
│ Cliente recibe: 201 CREATED                          │
│ Incluye: fabricTxId (prueba de auditoría)            │
└──────────────────────────────────────────────────────┘
```

---

## 🧪 Testing

### Opción 1: Script Automatizado (RECOMENDADO)
```bash
chmod +x test_siladocs.sh
./test_siladocs.sh
```

### Opción 2: Postman
```bash
# Importar: SilaDocs_Fabric_Postman.json
# Configurar variables
# Ejecutar requests pre-configurados
```

### Opción 3: Curl Manual
```bash
# Ver TESTING_GUIDE.sh para comandos completos
bash TESTING_GUIDE.sh
```

---

## 📊 Estado de APIs

| Endpoint | Descrición | Estado |
|----------|-----------|--------|
| `GET /health` | Health check general | ✅ |
| `GET /health/fabric` | Health check Fabric | ✅ |
| `POST /auth/login` | Obtener JWT token | ✅ |
| `POST /api/syllabi/upload` | Upload sílabo (Fabric) | ⭐ Principal |
| `POST /api/documents/upload` | Upload documento | ✅ |
| `GET /api/documents/{id}` | Obtener documento | ✅ |

---

## 🐛 Troubleshooting

### "Fabric API no disponible"
```bash
# Verificar que middleware está corriendo
curl http://127.0.0.1:8000/health

# Si no: iniciar en otra terminal
cd ../siladocs-fabric-middleware
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

### "PostgreSQL connection refused"
```bash
# Iniciar PostgreSQL
docker-compose up -d postgresql

# Esperar 10 segundos
sleep 10

# Verificar
psql -h localhost -U siladocs_user -d siladocs -c "SELECT 1"
```

### "Timeout en Fabric"
```bash
# Aumentar timeouts en application-fabric.yml
blockchain:
  fabric:
    api:
      timeout:
        connect: 30000  # Aumentar
        read: 60000     # Aumentar
```

**Para más ayuda:** Ver `SETUP_FABRIC.md` sección "Troubleshooting"

---

## 📦 Stack Tecnológico

| Layer | Tecnología |
|-------|-----------|
| Backend | Spring Boot 3.5.5, Java 21 |
| Blockchain | Hyperledger Fabric (via REST) |
| Almacenamiento | MinIO (S3-compatible) |
| Base de Datos | PostgreSQL |
| Seguridad | JWT + Spring Security + BCrypt |
| Testing | JUnit 5, Mockito |

---

## 🔐 Seguridad

- ✅ JWT tokens (expiration 24h configurable)
- ✅ BCrypt password hashing
- ✅ CORS configurado
- ✅ Transacciones blockchain immutables
- ✅ SHA-256 para validación de integridad

---

## 📝 Configuración

### Variables de Entorno Principales
```bash
FABRIC_API_URL=http://127.0.0.1:8000
JWT_SECRET=tu_secret_seguro_aqui

# Base de datos
POSTGRES_DB=siladocs
POSTGRES_USER=siladocs_user
POSTGRES_PASSWORD=siladocs_password

# MinIO
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_BUCKET=syllabi
```

### Profiles de Spring
```bash
# Desarrollo local (Fabric)
java -jar target/siladocs-backend.jar --spring.profiles.active=fabric

# Producción (requiere variables de entorno)
java -jar target/siladocs-backend.jar --spring.profiles.active=fabric
```

---

## 🎓 Flujo de Ejemplo

1. **Admin usa Postman para registrar sílabo**
   ```json
   POST /api/syllabi/upload
   Authorization: Bearer eyJhb...
   Form: courseId=1, action=create, file=syllabus.pdf
   ```

2. **Backend calcula SHA-256**
   ```
   abc123def456... (64 caracteres hex)
   ```

3. **Sube a MinIO**
   ```
   URL: http://minio:9000/syllabi/course-1/uuid-timestamp.pdf
   ```

4. **Registra en Fabric**
   ```json
   POST http://fabric-middleware:8000/registrar-hash
   {
     "curso_id": "1",
     "file_hash": "abc123...",
     "issuer": "admin@siladocs.com",
     "date": "2026-04-08"
   }
   ```

5. **Fabric devuelve txId (prueba de auditoría)**
   ```json
   {
     "status": "success",
     "txId": "tx-12345-uuid"
   }
   ```

6. **Backend guarda en PostgreSQL**
   ```sql
   INSERT INTO syllabus 
   (course_id, current_version, current_hash, file_url, fabric_tx_id)
   VALUES (1, 1, 'abc123...', 'http://minio:9000/...', 'tx-12345-uuid')
   ```

7. **Cliente recibe confirmación**
   ```json
   {
     "syllabusId": 1,
     "version": 1,
     "fabricTxId": "tx-12345-uuid",
     "status": "success"
   }
   ```

---

## 🚀 Despliegue

### Desarrollo
```bash
docker-compose up -d postgresql minio
java -jar target/siladocs-backend.jar --spring.profiles.active=fabric
```

### Producción (Docker)
```bash
docker build -t siladocs-backend:latest .
docker run -d \
  -e FABRIC_API_URL=https://fabric-api.prod.com \
  -e POSTGRES_URL=postgres://db-prod:5432/siladocs \
  siladocs-backend:latest
```

---

## 📞 Soporte

- 📖 Documentación completa: **SETUP_FABRIC.md**
- 🧪 Testing guide: **TESTING_GUIDE.sh**
- ⚙️ Configuración: **application-fabric.yml**
- 🔍 Tests unitarios: **BlockchainServiceTest.java**

---

## 📄 Licencia

Proyecto SilaDocs - Derechos reservados 2026

---

## 🎉 Versiones

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0-Fabric | Abril 2026 | Migración de Ethereum a Hyperledger Fabric |
| 0.9 | Mar 2026 | Pre-release con Web3j |

---

**Última actualización: Abril 8, 2026**
