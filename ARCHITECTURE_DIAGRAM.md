# 🔄 DIAGRAMA DE FLUJO - ARQUITECTURA DESPUÉS DEL REFACTOR

## Flujo de Datos: Validación de AccessCode

```
┌─────────────────────────────────────────────────────────────────┐
│                     HTTP Request                                │
│            POST /auth/validate-code                             │
│         { "code": "ABC-123-XYZ" }                              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
         ┌──────────────────────────┐
         │   AuthController         │
         │  (application/controller)│
         └────────────┬─────────────┘
                      │
                      │ calls validateCode()
                      ▼
         ┌──────────────────────────────────┐
         │   AccessCodeService              │
         │  (application/service)           │
         │                                  │
         │ - validateCode(code: String)    │
         │ - markAsUsed(accessCode)        │
         └────────────┬────────────────────┘
                      │
                      │ queries repository.findByCode()
                      ▼
    ┌──────────────────────────────────────────────┐
    │  AccessCodeRepository                        │
    │ (domain/repository) - @Repository            │
    │                                              │
    │ interface AccessCodeRepository extends      │
    │     JpaRepository<AccessCode, UUID> {        │
    │     Optional<AccessCode> findByCode(code)   │
    │ }                                            │
    └────────────┬───────────────────────────────┘
                 │
                 │ Spring Data JPA generates SQL
                 ▼
    ┌──────────────────────────────────────────────┐
    │  AccessCode (JPA Entity)                     │
    │ (domain/entity)                              │
    │                                              │
    │ @Entity                                      │
    │ @Table(name = "access_codes")               │
    │ public class AccessCode {                   │
    │     @Id private UUID id;                    │
    │     @Column(unique=true) String code;       │
    │     String institutionName;                 │
    │     Instant expiresAt;   ← NEW: Instant    │
    │     boolean used;                           │
    │     Instant createdAt;   ← NEW: Instant    │
    │                                              │
    │     public boolean isExpired() {...}        │
    │     public boolean isValid() {...}          │
    │ }                                            │
    └────────────┬───────────────────────────────┘
                 │
                 │ Spring Data JPA executes
                 │ SELECT * FROM access_codes 
                 │ WHERE code = 'ABC-123-XYZ'
                 ▼
    ┌──────────────────────────────────────────────┐
    │  PostgreSQL Database                         │
    │                                              │
    │  Table: access_codes                         │
    │  ├─ id (UUID)                               │
    │  ├─ code (VARCHAR UNIQUE)                   │
    │  ├─ institution_name (VARCHAR)              │
    │  ├─ expires_at (TIMESTAMP)   ← Instant     │
    │  ├─ used (BOOLEAN)                          │
    │  └─ created_at (TIMESTAMP)   ← Instant     │
    │                                              │
    │  Indices (NEW):                             │
    │  ├─ idx_access_codes_code                   │
    │  ├─ idx_access_codes_used                   │
    │  └─ idx_access_codes_expires_at             │
    └────────────┬───────────────────────────────┘
                 │
                 │ Devuelve resultado de BD
                 ▼
    Row{id, code, institutionName, expiresAt, used, createdAt}
                 │
                 │ Hibernate mapea a entidad AccessCode
                 │ (sin mapper manual, automático)
                 ▼
    ┌──────────────────────────────────────────────┐
    │  AccessCode Entity Instance                  │
    │ (domain/entity)                              │
    │                                              │
    │ accessCode.getId()         → UUID            │
    │ accessCode.getCode()       → String          │
    │ accessCode.isExpired()     → boolean call   │
    │ accessCode.isValid()       → boolean call   │
    └────────────┬───────────────────────────────┘
                 │
                 │ Validation Logic in Service
                 ▼
    ┌──────────────────────────────────────────────┐
    │  AccessCodeService.validateCode()            │
    │                                              │
    │  if (accessCode.isExpired())                │
    │    throw RuntimeException("Expirado")       │
    │                                              │
    │  if (accessCode.isUsed())                   │
    │    throw RuntimeException("Ya usado")       │
    │                                              │
    │  return accessCode; ← RETORNA ENTIDAD JPA  │
    └────────────┬───────────────────────────────┘
                 │
                 ▼
    ┌──────────────────────────────────────────────┐
    │  AuthController                              │
    │  (application/controller)                    │
    │                                              │
    │  return ResponseEntity.ok({                 │
    │    "message": "Código válido"               │
    │  });                                         │
    └────────────┬───────────────────────────────┘
                 │
                 ▼
         ┌──────────────────────────┐
         │   HTTP Response 200 OK   │
         │  { "message": "..." }   │
         └──────────────────────────┘
```

---

## Flujo de Datos: Registro con AccessCode (COMPLETO)

```
POST /auth/register
{ "accessCode": "ABC-123", "fullName": "John", "email": "john@test.com", "password": "..." }
                    │
                    ▼
        ┌──────────────────────────┐
        │   AuthController         │
        │  POST /auth/register     │
        └────────────┬─────────────┘
                     │
                     │ calls authService.registerInstitution()
                     ▼
        ┌──────────────────────────────────┐
        │   AuthService                    │
        │ (application/service)            │
        │                                  │
        │ @Transactional                   │
        │ public String registerInstitution(...) {
        └────────────┬────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
    ┌─────────────────┐   ┌──────────────────────┐
    │ Step 1:         │   │ Step 2:              │
    │ Validate Code   │   │ Create Institution   │
    └────────┬────────┘   └──────────┬───────────┘
             │                       │
             │ accessCodeService    │ institutionRepository
             │   .validateCode()    │   .save(institution)
             │                       │
             ▼                       ▼
    AccessCode entity    Institution entity saved
    (from DB via repo)   with ID from DB
             │                       │
             └───────────┬───────────┘
                         │
         ┌───────────────┴────────────────┐
         │                                │
         ▼                                ▼
    ┌──────────────────┐   ┌──────────────────────┐
    │ Step 3:          │   │ Step 4:              │
    │ Create Admin     │   │ Mark Code as Used    │
    │ User             │   │ accessCodeService    │
    └─────────┬────────┘   │   .markAsUsed()      │
              │            └──────────┬───────────┘
              │ userRepository        │
              │   .save(admin)        │ repository
              │                       │   .save(accessCode)
              ▼                       ▼
         User entity saved    AccessCode.used = true
         with ADMIN role      (updated in DB)
              │                       │
              └───────────┬───────────┘
                          │
                          ▼
            ┌──────────────────────────┐
            │ Step 5:                  │
            │ Generate JWT Token       │
            │ jwtUtil.generateToken()  │
            └───────────┬──────────────┘
                        │
                        ▼
            ┌──────────────────────────┐
            │ HTTP Response 200 OK     │
            │ {                        │
            │   "token": "jwt...",     │
            │   "email": "...",        │
            │   "role": "ROLE_ADMIN",  │
            │   "institutionId": "..." │
            │ }                        │
            └──────────────────────────┘
```

---

## Arquitectura Limpia: Capas y Responsabilidades

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│  AuthController (REST endpoints)                               │
│  ├─ POST /auth/validate-code                                  │
│  └─ POST /auth/register                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │ (DTOs: ValidateCodeRequest, etc)
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                  APPLICATION LAYER                              │
│  Services (DTO → Entity conversion, Transactional logic)       │
│  ├─ AccessCodeService (validateCode, markAsUsed)              │
│  ├─ AuthService (registerInstitution, login, etc)             │
│  └─ [Other Services]                                          │
└────────────────────┬────────────────────────────────────────────┘
                     │ (domain.entity.AccessCode)
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                                 │
│  ├─ Entities (Business Logic, Validation)                     │
│  │  └─ AccessCode (isExpired, isValid)                        │
│  │     ├─ UUID id                                             │
│  │     ├─ String code                                         │
│  │     ├─ Instant expiresAt                                   │
│  │     └─ boolean isValid()                                   │
│  │                                                             │
│  └─ Repositories (Contracts/Abstractions)                     │
│     └─ AccessCodeRepository (JpaRepository interface)         │
│        ├─ findByCode(String)                                  │
│        └─ CRUD operations inherited from JpaRepository        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE LAYER                             │
│  ├─ Spring Data JPA Implementation (automatic)                │
│  │  └─ Hibernate SQL generation                              │
│  │                                                             │
│  └─ Database (PostgreSQL)                                    │
│     └─ access_codes table with indices                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Flujo de Timestamps: LocalDateTime → Instant

```
┌──────────────────────────────────────────────────────────────────┐
│ ANTES (Inconsistente)                                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ domain/model/AccessCode (POJO)                                 │
│   private Instant expiresAt;       ← Instant (UTC)            │
│   private Instant createdAt;       ← Instant (UTC)            │
│                                                                  │
│ domain/entity/AccessCode (JPA)                                 │
│   private LocalDateTime expiresAt; ← LocalDateTime (sin TZ)    │
│   private LocalDateTime createdAt; ← LocalDateTime (sin TZ)    │
│                                                                  │
│ infrastructure/persistence/entity/AccessCodeEntity (JPA)       │
│   private Instant expiresAt;       ← Instant (UTC)            │
│   private Instant createdAt;       ← Instant (UTC)            │
│                                                                  │
│ ❌ PROBLEMA: 3 representaciones diferentes del mismo concepto  │
│ ❌ Requería conversiones en mapper                             │
│ ❌ Riesgo de bugs con timezones                               │
└──────────────────────────────────────────────────────────────────┘

                              │
                              │ REFACTOR
                              ▼

┌──────────────────────────────────────────────────────────────────┐
│ DESPUÉS (Consistente)                                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ domain/entity/AccessCode (ÚNICA Entidad JPA)                  │
│   @Column(name = "expires_at")                                │
│   private Instant expiresAt;       ← Instant (UTC)            │
│   @Column(name = "created_at")                                │
│   private Instant createdAt;       ← Instant (UTC)            │
│   @PrePersist                                                 │
│   public void prePersist() {                                  │
│     this.createdAt = Instant.now(); ← UTC automático         │
│   }                                                            │
│                                                                  │
│ ✅ BENEFICIO: Single source of truth                          │
│ ✅ Hibernate convierte automáticamente TIMESTAMP ↔ Instant    │
│ ✅ Sin conversiones manuales                                  │
│ ✅ Siempre UTC (no ambigüedad de timezones)                  │
│ ✅ Compatible con PostgreSQL TIMESTAMP                        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Transaccionalidad: Antes vs Después

```
┌──────────────────────────────────────────────────────────┐
│ ANTES (Complejo)                                         │
├──────────────────────────────────────────────────────────┤
│                                                          │
│ @Transactional                                          │
│ public AccessCode validateCode(String code) {          │
│   AccessCodeEntity entity = repository              │
│     .findByCode(code)...              ← Entity infrast │
│   AccessCode domain =                                │
│     mapper.toDomain(entity)     ← Conversión manual   │
│   if (domain.isExpired()) throw...                   │
│   return domain;  ← Retorna modelo, no entity       │
│ }                                                      │
│                                                       │
│ ❌ Mapeo manual (riesgo de inconsistencias)         │
│ ❌ Dos tipos diferentes en capas                   │
│ ❌ Potencial desincronización Entity ↔ Domain      │
└──────────────────────────────────────────────────────────┘

                        │
                        │ REFACTOR
                        ▼

┌──────────────────────────────────────────────────────────┐
│ DESPUÉS (Limpio)                                         │
├──────────────────────────────────────────────────────────┤
│                                                          │
│ @Transactional(readOnly = true)                        │
│ public AccessCode validateCode(String code) {         │
│   AccessCode accessCode = repository              │
│     .findByCode(code)...         ← Entity JPA directo│
│   if (accessCode.isExpired())                     │
│     throw...                                      │
│   return accessCode;  ← Retorna MISMA entidad    │
│ }                                                      │
│                                                       │
│ ✅ Sin mapeos manuales (Hibernate lo hace)         │
│ ✅ Un único tipo en todas las capas               │
│ ✅ Siempre sincronizado (es la misma instancia)   │
│ ✅ Transacciones JPA nativas                      │
└──────────────────────────────────────────────────────────┘
```

---

## Resumen Visual: Antes vs Después

```
ANTES (3 versiones de AccessCode)        DESPUÉS (1 versión único)

┌──────────────────────┐                 ┌──────────────────────┐
│ domain/model/        │                 │ domain/entity/       │
│ AccessCode (POJO)    │                 │ AccessCode (JPA)     │
│ ├─ id                │                 │ ├─ @Entity           │
│ ├─ code              │                 │ ├─ id                │
│ └─ isValid()         │                 │ ├─ code              │
└──────────────────────┘                 │ ├─ @Column(...)      │
           │                             │ └─ isValid()         │
           │ (MAPPED BY)                 └──────────────────────┘
           │                                      ▲
┌──────────────────────────────┐                 │
│ infrastructure/persistence/  │                 │
│ entity/AccessCodeEntity (JPA)│        (USED DIRECTLY)
│ ├─ @Entity                   │                 │
│ ├─ id                        │    ┌────────────┴───────────┐
│ └─ @Column(...)              │    │                        │
└──────────────────────────────┘    ▼                        ▼
           ▲                  ┌─────────────────┐ ┌──────────────────┐
           │                 │ AccessCodeService
           │ (USED BY)        │ (application)   │ │ AuthService      │
           │                 └─────────────────┘ │ (application)    │
  ┌────────┴───────────┐                         └──────────────────┘
  │                    │                                  │
┌─────────────────┐ ┌──────────────────┐                 │
│ AccessCodeMapper│ │ AccessCodeJpaRep │    (USES DIRECTLY NOW)
│ (NOT NEEDED)    │ │ (infrastructure) │                 │
└─────────────────┘ └──────────────────┘                 │
                                                         │
                                        ┌────────────────┴────────────┐
                                        │                             │
                                ┌───────▼─────────┐    ┌──────────────▼───┐
                                │ AccessCodeRep   │    │ Flyway Migration│
                                │ (domain/repo)   │    │ V001__..sql     │
                                │ @Repository     │    └─────────────────┘
                                └────────┬────────┘
                                         │
                                         ▼
                                ┌──────────────────┐
                                │ PostgreSQL       │
                                │ access_codes     │
                                └──────────────────┘
```

---

**CONCLUSIÓN**: Arquitectura simplificada, más mantenible, sin duplicaciones.
