package com.siladocs.application.service;

import com.siladocs.application.dto.SyllabusResponse;
import com.siladocs.application.exception.BlockchainException;
import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.entity.SyllabusEntity;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusHistoryLogRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusJpaRepository;
import com.siladocs.infrastructure.storage.StorageService;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de Sílabos (SyllabusService) - Refactorizado para Hyperledger
 * Fabric.
 *
 * ARQUITECTURA (Clean Architecture):
 * ├── Layer: Application Service (SLL)
 * ├── Dependencies: BlockchainService, StorageService, Repositories
 * └── Purpose: Orquestar el flujo de upload de sílabos
 *
 * FLUJO ESTRICTO DE UPLOAD (SilaDocs Fabric):
 * ┌─────────────────────────────────────────────────────┐
 * │ 1. RECIBIR ENTRADA │
 * │ - courseId, MultipartFile (archivo físico) │
 * ├─────────────────────────────────────────────────────┤
 * │ 2. CALCULAR SHA-256 │
 * │ - Hash del contenido del archivo │
 * ├─────────────────────────────────────────────────────┤
 * │ 3. SUBIR A MinIO │
 * │ - StorageService.uploadFile(...) │
 * │ - Obtener URL pública │
 * ├─────────────────────────────────────────────────────┤
 * │ 4. REGISTRAR EN FABRIC (CRÍTICO) ⛓️ │
 * │ - BlockchainService.registerSyllabusInFabric() │
 * │ - Si falla → excepción (rollback automático) │
 * ├─────────────────────────────────────────────────────┤
 * │ 5. PERSISTIR EN PostgreSQL │
 * │ - Solo si Fabric fue exitoso │
 * │ - @Transactional revierte TODO si falla │
 * └─────────────────────────────────────────────────────┘
 */
@Service
public class SyllabusService {

    private static final Logger log = LoggerFactory.getLogger(SyllabusService.class);

    private final SyllabusJpaRepository syllabusRepo;
    private final CourseJpaRepository courseRepo;
    private final BlockchainService blockchainService;
    private final StorageService storageService;
    private final SyllabusHistoryLogRepository historyRepo;
    private final UserRepository userRepo;

    public SyllabusService(SyllabusJpaRepository syllabusRepo,
            CourseJpaRepository courseRepo,
            BlockchainService blockchainService,
            StorageService storageService,
            SyllabusHistoryLogRepository historyRepo,
            UserRepository userRepo) {
        this.syllabusRepo = syllabusRepo;
        this.courseRepo = courseRepo;
        this.blockchainService = blockchainService;
        this.storageService = storageService;
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
    }

    /**
     * Sube un sílabo siguiendo el flujo estricto de SilaDocs Fabric.
     *
     * @param courseId     ID del curso
     * @param syllabusFile Archivo del sílabo (MultipartFile)
     * @param action       Acción (create, update, etc.)
     * @throws BlockchainException      Si Fabric falla
     * @throws RuntimeException         Si hay error en persistencia/storage
     * @throws IllegalArgumentException Si parámetros son inválidos
     */
    @Transactional
    public SyllabusResponse uploadSyllabus(Long courseId, MultipartFile syllabusFile, String action) {
        String userEmail = getAuthenticatedUserEmail();

        log.info("🔄 INICIANDO UPLOAD DE SÍLABO: courseId={}, user={}, action={}, file={}",
                courseId, userEmail, action, syllabusFile.getOriginalFilename());

        try {
            // ===== PASO 1: VALIDAR ENTRADA =====
            validateInput(courseId, syllabusFile);
            log.debug("✅ Validación de entrada exitosa");

            // ===== PASO 2: VERIFICAR QUE EL CURSO EXISTE =====
            CourseEntity course = courseRepo.findById(courseId)
                    .orElseThrow(() -> {
                        log.error("❌ Curso no encontrado: courseId={}", courseId);
                        return new IllegalArgumentException("Curso no encontrado");
                    });
            log.debug("✅ Curso encontrado: {}", course.getId());

            // ===== PASO 3: LEER CONTENIDO DEL ARCHIVO =====
            byte[] fileBytes = readFileBytes(syllabusFile);
            log.debug("✅ Archivo leído: {} bytes", fileBytes.length);

            // ===== PASO 4: CALCULAR SHA-256 =====
            String fileHash = DigestUtils.sha256Hex(fileBytes);
            log.info("📝 Hash SHA-256 calculado: {}", fileHash.substring(0, 12) + "...");

            // ===== PASO 5: VERIFICAR CAMBIOS (OPTIMIZACIÓN) =====
            var existingSyllabus = syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(courseId);
            if (existingSyllabus.isPresent() && fileHash.equals(existingSyllabus.get().getCurrentHash())) {
                log.info("⏭️ OMITIENDO UPLOAD: Hash idéntico (sin cambios reales para courseId={})", courseId);
                SyllabusEntity existing = existingSyllabus.get();
                return new SyllabusResponse(
                        existing.getId(),
                        existing.getCourse().getId(),
                        existing.getCourse().getName(),
                        existing.getCourse().getCode(),
                        existing.getFileUrl(),
                        syllabusFile.getSize(),
                        existing.getCurrentHash(),
                        existing.getStatus(),
                        existing.getCreatedAt(),
                        existing.getFabricTxId()
                );
            }
            log.debug("✅ Contenido del sílabo ha cambiado o es nuevo");

            // ===== PASO 6: SUBIR A MinIO =====
            String folderPath = String.format("/syllabi/course-%d/", courseId);
            String fileUrl = uploadToMinIO(syllabusFile, fileBytes, folderPath);
            log.info("☁️ Archivo subido a MinIO: {}", fileUrl);

            // ===== PASO 7: CREAR/ACTUALIZAR ENTIDAD EN MEMORIA (SIN PERSISTIR) =====
            SyllabusEntity syllabus;
            if (existingSyllabus.isEmpty()) {
                // Crear nuevo
                syllabus = new SyllabusEntity();
                syllabus.setCourse(course);
                syllabus.setCreatedAt(Instant.now());
                syllabus.setCurrentVersion(0);
                syllabus.setLastChainHash("0000000000000000000000000000000000000000000000000000000000000000");
                log.debug("✅ Creando nuevo sílabo para courseId={}", courseId);
            } else {
                // Actualizar existente
                syllabus = existingSyllabus.get();
                log.debug("✅ Actualizando sílabo existente: id={}", syllabus.getId());
            }

            int nextVersion = syllabus.getCurrentVersion() + 1;
            syllabus.setFileUrl(fileUrl);
            syllabus.setCurrentHash(fileHash);
            syllabus.setCurrentVersion(nextVersion);
            syllabus.setStatus(action);
            syllabus.setUpdatedAt(Instant.now());
            log.debug("✅ Entidad preparada: version={}, hash_prefix={}", nextVersion, fileHash.substring(0, 12));

            // ===== PASO 8: REGISTRAR EN FABRIC (CRÍTICO - PUNTO DE NO RETORNO) ⛓️ =====
            String txId;
            try {
                log.info("⛓️ REGISTRANDO EN HYPERLEDGER FABRIC...");
                txId = blockchainService.registerSyllabusInFabric(
                        String.valueOf(courseId),
                        fileHash,
                        userEmail,
                        action,
                        syllabusFile.getOriginalFilename(),
                        syllabusFile.getContentType(),
                        syllabusFile.getSize(),
                        userEmail,
                        getInstitutionNameFromUser(userEmail));
                log.info("✅ FABRIC EXITOSO: txId={}", txId);
            } catch (BlockchainException e) {
                log.error("❌ FALLO CRÍTICO EN FABRIC: {}. La transacción será revertida.", e.getMessage());
                throw new BlockchainException(
                        "Blockchain registró error: " + e.getMessage() +
                                ". Upload revertido (MinIO podría mantener copia huérfana).",
                        e);
            }

            // ===== PASO 9: PERSISTIR EN PostgreSQL (SOLO SI FABRIC EXITOSO) =====
            syllabus.setFabricTxId(txId);
            try {
                SyllabusEntity saved = syllabusRepo.save(syllabus);
                log.info("✅ SÍLABO GUARDADO EN PostgreSQL: syllabusId={}, version={}, txId={}",
                        saved.getId(), saved.getCurrentVersion(), txId);

                return new SyllabusResponse(
                        saved.getId(),
                        saved.getCourse().getId(),
                        saved.getCourse().getName(),
                        saved.getCourse().getCode(),
                        saved.getFileUrl(),
                        syllabusFile.getSize(),
                        saved.getCurrentHash(),
                        saved.getStatus(),
                        saved.getCreatedAt(),
                        saved.getFabricTxId()
                );
            } catch (Exception e) {
                log.error("❌ Error al guardar en PostgreSQL (Fabric ya registró): {}", e.getMessage());
                throw new RuntimeException(
                        "Error al guardar en BD (Fabric ya registrado): " + e.getMessage(),
                        e);
            }

        } catch (BlockchainException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ ERROR INESPERADO: {}", e.getMessage(), e);
            throw new RuntimeException("Error inesperado al subir sílabo: " + e.getMessage(), e);
        }
    }

    /**
     * Valida los parámetros de entrada.
     */
    private void validateInput(Long courseId, MultipartFile file) {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("courseId debe ser un número positivo");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío o nulo");
        }
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("Nombre de archivo vacío");
        }
        long maxSize = 50 * 1024 * 1024; // 50 MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Archivo demasiado grande (máx 50 MB)");
        }
    }

    /**
     * Lee los bytes del archivo MultipartFile.
     */
    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.error("Error al leer bytes del archivo: {}", e.getMessage());
            throw new RuntimeException("Error al leer archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Sube el archivo a MinIO y retorna la URL pública.
     */
    private String uploadToMinIO(MultipartFile file, byte[] fileBytes, String folderPath) {
        try {
            return storageService.uploadBytes(
                    fileBytes,
                    file.getOriginalFilename(),
                    folderPath,
                    file.getContentType());
        } catch (Exception e) {
            log.error("Error al subir a MinIO: {}", e.getMessage());
            throw new RuntimeException("Error al subir a MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el email del usuario autenticado.
     */
    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            log.warn("⚠️ Usuario no autenticado. Usando 'system@siladocs.com'");
            return "system@siladocs.com";
        }
        return auth.getName();
    }
        /**
     * Obtiene el nombre de la institución del usuario autenticado.
     */
    private String getInstitutionNameFromUser(String userEmail) {
        try {
            var user = userRepo.findByEmail(userEmail);
            if (user.isPresent() && user.get().getInstitutionId() != null) {
                // Para esta versión, retornamos genérico
                // En producción, integrar con InstitutionRepository
                return "Institución-" + user.get().getInstitutionId();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener institución para {}: {}", userEmail, e.getMessage());
        }
        return "Institución desconocida";
    }

    /**
     * Obtiene todos los sílabos con la información del curso asociado.
     */
    public List<SyllabusResponse> getAllSyllabi() {
        try {
            return syllabusRepo.findAll().stream()
                    .map(syllabus -> new SyllabusResponse(
                            syllabus.getId(),
                            syllabus.getCourse().getId(),
                            syllabus.getCourse().getName(),
                            syllabus.getCourse().getCode(),
                            syllabus.getFileUrl(),
                            0L, // fileSize not stored at entity level
                            syllabus.getCurrentHash(),
                            syllabus.getStatus(),
                            syllabus.getCreatedAt(),
                            syllabus.getFabricTxId()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error al obtener todos los sílabos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sílabos: " + e.getMessage(), e);
        }
    }
}
