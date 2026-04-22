# ✅ GUÍA COMPLETA DE TESTING - REFACTOR ACCESSCODE

## 📊 Resumen del Refactor Realizado

### ✅ Cambios Implementados:

1. **Entidad JPA Única**: `domain/entity/AccessCode` con `Instant` timestamps
2. **Repositorio JPA Directo**: `domain/repository/AccessCodeRepository` como `@Repository`
3. **Servicio Simplificado**: `AccessCodeService` sin mapper, usando entidad directamente
4. **Eliminadas Duplicaciones**:
   - ❌ `domain/model/AccessCode` (POJO sin usar)
   - ❌ `infrastructure/persistence/entity/AccessCodeEntity` (Entity duplicada)
   - ❌ `infrastructure/persistence/jparepository/AccessCodeJpaRepository` (Interfaz duplicada)
   - ❌ `infrastructure/persistence/mapper/AccessCodeMapper` (Mapper innecesario)
5. **Migración Flyway**: `V001__AccessCode_schema_cleanup.sql` con índices
6. **Dependencias**: Agregadas `flyway-core` y `flyway-database-postgresql` al pom.xml

---

## 🧪 PLAN DE TESTING PASO A PASO

### FASE 1: Verificación Local (Sin Docker)

#### Paso 1.1: Compilación Exitosa ✅
```bash
cd c:\Personal\Universidad\TP2\GitHub\siladocs-backend
.\mvnw.cmd clean compile -DskipTests
```
**Resultado esperado**: BUILD SUCCESS (sin errores de compilación)

---

#### Paso 1.2: Verificar que no hay imports rotos
```bash
.\mvnw.cmd dependency:tree | findstr AccessCode
```
**Resultado esperado**: No aparecen referencias a clases eliminadas

---

#### Paso 1.3: Ejecutar tests unitarios
```bash
.\mvnw.cmd test
```
**Resultado esperado**: Todos los tests pasan (si existen tests de AccessCode)

---

### FASE 2: Verificación de Estructura (Código Estático)

#### Paso 2.1: Verificar que la entidad JPA está correcta
Ubicación: `src/main/java/com/siladocs/domain/entity/AccessCode.java`

Verificar:
- ✅ Clase anotada con `@Entity`
- ✅ Tabla `access_codes` mapeada
- ✅ Timestamps usan `Instant` (no `LocalDateTime`)
- ✅ Métodos `isExpired()` y `isValid()`
- ✅ `@PrePersist` setea `createdAt = Instant.now()`

---

#### Paso 2.2: Verificar que el repositorio está correcto
Ubicación: `src/main/java/com/siladocs/domain/repository/AccessCodeRepository.java`

Verificar:
- ✅ Interfaz anotada con `@Repository`
- ✅ Extiende `JpaRepository<AccessCode, UUID>`
- ✅ Método `findByCode(String code)`
- ✅ Tipo genérico es `domain.entity.AccessCode` (no `infrastructure.persistence.entity`)

---

#### Paso 2.3: Verificar que AccessCodeService usa la entidad directamente
Ubicación: `src/main/java/com/siladocs/application/service/AccessCodeService.java`

Verificar:
- ✅ Inyecta `AccessCodeRepository` (no `AccessCodeJpaRepository`)
- ✅ NO importa `AccessCodeMapper`
- ✅ NO importa `AccessCodeEntity`
- ✅ Retorna `domain.entity.AccessCode` (no `domain.model.AccessCode`)
- ✅ Método `validateCode()` consulta directamente al repositorio sin mapeo
- ✅ Método `markAsUsed()` actualiza la entidad directamente

---

#### Paso 2.4: Verificar que no quedan referencias obsoletas
En la terminal, ejecutar:
```bash
# Buscar cualquier referencia a clases eliminadas
cd src/main/java
findstr /r /c:"domain\.model\.AccessCode\|AccessCodeEntity\|AccessCodeMapper\|AccessCodeJpaRepository" *.java
# Resultado esperado: 0 matches
```

---

### FASE 3: Ejecución en Docker (Testing Real)

#### Paso 3.1: Construir imagen Docker
```bash
cd c:\Personal\Universidad\TP2\GitHub\siladocs-backend
.\mvnw.cmd clean package -DskipTests
docker build -t siladocs-backend:latest .
```
**Resultado esperado**: Build exitoso, imagen creada

---

#### Paso 3.2: Levantar contenedores con docker-compose
```bash
docker-compose up -d
# Esperar 30 segundos para que la aplicación arranque
Start-Sleep -Seconds 30
```
**Resultado esperado**: Contenedores corriendo sin errores en los logs

---

#### Paso 3.3: Revisar logs del backend
```bash
docker logs siladocs-backend-app -f
```
**Resultado esperado**: 
- ✅ Hibernate crea/actualiza tabla `access_codes`
- ✅ Flyway ejecuta migraciones (V001__AccessCode_schema_cleanup.sql)
- ✅ Aplicación inicia en puerto 8080
- ✅ Sin errores de `ClassNotFoundException` o imports rotos

---

#### Paso 3.4: Verificar que la BD tiene los índices correctos
```bash
docker exec -it siladocs-backend-postgres-1 psql -U postgres -d siladocs_db -c "\d access_codes"
```

**Resultado esperado**: 
```
       Table "public.access_codes"
     Column      |            Type             
-----------------+-----------------------------
 id              | uuid
 code            | character varying(255)
 institution_name| character varying(255)
 expires_at      | timestamp without time zone
 used            | boolean
 created_at      | timestamp without time zone
 
Indexes:
    "access_codes_pkey" PRIMARY KEY, btree (id)
    "idx_access_codes_code" btree (code)          ← Índice nuevo
    "idx_access_codes_expires_at" btree (expires_at)  ← Índice nuevo
    "idx_access_codes_used" btree (used)          ← Índice nuevo
```

---

### FASE 4: Testing de Endpoints (Validación Funcional)

#### Paso 4.1: Crear un código de acceso para test
```bash
# Conectarse a la BD de PostgreSQL
docker exec -it siladocs-backend-postgres-1 psql -U postgres -d siladocs_db

# Ejecutar en psql:
INSERT INTO access_codes (id, code, institution_name, expires_at, used, created_at)
VALUES (
    gen_random_uuid(),
    'TEST-CODE-001',
    'Test Institution',
    NOW() + INTERVAL '1 day',
    FALSE,
    NOW()
);

# Verificar que se insertó
SELECT * FROM access_codes WHERE code = 'TEST-CODE-001';
```

**Resultado esperado**: Una fila insertada correctamente

---

#### Paso 4.2: Probar endpoint `/auth/validate-code`
```bash
# En Postman o con curl:
POST http://localhost:8080/auth/validate-code
Content-Type: application/json

{
  "code": "TEST-CODE-001"
}
```

**Resultado esperado**: 
```json
{
  "message": "Código válido"
}
```

---

#### Paso 4.3: Probar endpoint `/auth/register` (flow completo)
```bash
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "accessCode": "TEST-CODE-001",
  "fullName": "Test User",
  "email": "test@example.com",
  "password": "SecurePass123!"
}
```

**Resultado esperado**: 
```json
{
  "token": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "email": "test@example.com",
  "role": "ROLE_ADMIN",
  "institutionId": "uuid-aqui"
}
```

---

#### Paso 4.4: Verificar que el código se marcó como usado
En la BD:
```sql
SELECT * FROM access_codes WHERE code = 'TEST-CODE-001';
```

**Resultado esperado**: 
- `used = true` ✅
- `id, code, institution_name, expires_at, created_at` intactos ✅

---

#### Paso 4.5: Intentar reutilizar el mismo código (debe fallar)
```bash
POST http://localhost:8080/auth/validate-code
Content-Type: application/json

{
  "code": "TEST-CODE-001"
}
```

**Resultado esperado**: 
```json
{
  "error": "El código ya fue utilizado"
}
```

---

### FASE 5: Validación de Transacciones y Consistencia

#### Paso 5.1: Crear código con fecha de expiración pasada
```sql
INSERT INTO access_codes (id, code, institution_name, expires_at, used, created_at)
VALUES (
    gen_random_uuid(),
    'EXPIRED-CODE',
    'Test Institution',
    NOW() - INTERVAL '1 hour',  -- Expirado hace 1 hora
    FALSE,
    NOW()
);
```

---

#### Paso 5.2: Intentar validar código expirado
```bash
POST http://localhost:8080/auth/validate-code
Content-Type: application/json

{
  "code": "EXPIRED-CODE"
}
```

**Resultado esperado**: 
```json
{
  "error": "El código ha expirado"
}
```

---

### FASE 6: Limpieza y Validación Final

#### Paso 6.1: Detener contenedores
```bash
docker-compose down
```

---

#### Paso 6.2: Verificar estructura de archivos
```bash
# NO debe existir ninguno de estos:
ls src/main/java/com/siladocs/domain/model/AccessCode.java          # ❌ Debe NO existir
ls src/main/java/com/siladocs/infrastructure/persistence/entity/AccessCodeEntity.java  # ❌ Debe NO existir
ls src/main/java/com/siladocs/infrastructure/persistence/mapper/AccessCodeMapper.java  # ❌ Debe NO existir
ls src/main/java/com/siladocs/infrastructure/persistence/jparepository/AccessCodeJpaRepository.java  # ❌ Debe NO existir

# DEBEN existir:
ls src/main/java/com/siladocs/domain/entity/AccessCode.java         # ✅ Debe existir
ls src/main/java/com/siladocs/domain/repository/AccessCodeRepository.java  # ✅ Debe existir
ls src/main/resources/db/migration/V001__AccessCode_schema_cleanup.sql  # ✅ Debe existir
```

---

## ✅ CHECKLIST FINAL

- [ ] Compilación exitosa sin errores
- [ ] Tests unitarios pasan
- [ ] Ninguna referencia a clases eliminadas
- [ ] Entidad JPA en `domain/entity/AccessCode`
- [ ] Repositorio JPA en `domain/repository/AccessCodeRepository`
- [ ] Servicio sin mapper
- [ ] Docker build exitoso
- [ ] Docker compose arranca sin errores
- [ ] Endpoint validate-code funciona
- [ ] Endpoint register funciona
- [ ] Código se marca como usado
- [ ] Código expirado rechazado
- [ ] Código ya usado rechazado
- [ ] Índices BD creados correctamente
- [ ] Migraciones Flyway ejecutadas

---

## 🎯 Resumen de Beneficios Logrados

✅ **Arquitectura más limpia**: Una única entidad JPA, sin duplicaciones  
✅ **Mejor mantenibilidad**: Menos clases, responsabilidades claras  
✅ **Transacciones correctas**: Hibernate maneja persistencia directamente  
✅ **Timestamps consistentes**: `Instant` en toda la arquitectura  
✅ **Documentación explícita**: Migraciones Flyway versionsadas  
✅ **Mejor rendimiento**: Índices BD agregados para búsquedas frecuentes  
✅ **Code quality**: Eliminadas clases obsoletas y mappers triviales  

---

**Fecha del refactor**: 18/04/2026  
**Versión**: siladocs-backend 0.0.1-SNAPSHOT  
**Status**: ✅ Listo para testing en producción
