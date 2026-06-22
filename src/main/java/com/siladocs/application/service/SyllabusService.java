package com.siladocs.application.service;

import com.siladocs.application.dto.SyllabusResponse;
import com.siladocs.application.exception.BlockchainException;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.entity.SyllabusEntity;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusHistoryLogRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusJpaRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SyllabusService {

    private static final Logger log = LoggerFactory.getLogger(SyllabusService.class);

    private final SyllabusJpaRepository syllabusRepo;
    private final CourseJpaRepository courseRepo;
    private final BlockchainService blockchainService;
    private final AzureBlobStorageService azureBlobStorageService;
    private final SyllabusHistoryLogRepository historyRepo;
    private final UserRepository userRepo;
    private final BlockchainEventEmitterService eventEmitter;
    private final SyllabusVersionService versionService;

    public SyllabusService(SyllabusJpaRepository syllabusRepo,
            CourseJpaRepository courseRepo,
            BlockchainService blockchainService,
            AzureBlobStorageService azureBlobStorageService,
            SyllabusHistoryLogRepository historyRepo,
            UserRepository userRepo,
            BlockchainEventEmitterService eventEmitter) {
        this(syllabusRepo, courseRepo, blockchainService, azureBlobStorageService,
                historyRepo, userRepo, eventEmitter, null);
    }

    @Autowired
    public SyllabusService(SyllabusJpaRepository syllabusRepo,
            CourseJpaRepository courseRepo,
            BlockchainService blockchainService,
            AzureBlobStorageService azureBlobStorageService,
            SyllabusHistoryLogRepository historyRepo,
            UserRepository userRepo,
            BlockchainEventEmitterService eventEmitter,
            SyllabusVersionService versionService) {
        this.syllabusRepo = syllabusRepo;
        this.courseRepo = courseRepo;
        this.blockchainService = blockchainService;
        this.azureBlobStorageService = azureBlobStorageService;
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
        this.eventEmitter = eventEmitter;
        this.versionService = versionService;
    }

    @Transactional
    public SyllabusResponse uploadSyllabus(Long courseId, MultipartFile syllabusFile, String action) {
        return uploadSyllabus(courseId, syllabusFile, action, null);
    }

    @Transactional
    public SyllabusResponse uploadSyllabus(Long courseId, MultipartFile syllabusFile, String action, String sessionId) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("UPLOAD: courseId={}, user={}, session={}", courseId, userEmail, sessionId);

        try {
            eventEmitter.emit(sessionId, "file_received",
                    "Archivo recibido: " + syllabusFile.getOriginalFilename(), "", 5);

            validateInput(courseId, syllabusFile);

            CourseEntity course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + courseId));

            eventEmitter.emit(sessionId, "hash_computing", "Calculando hash SHA-256...", "", 15);
            byte[] fileBytes = readFileBytes(syllabusFile);
            String fileHash = DigestUtils.sha256Hex(fileBytes);
            eventEmitter.emit(sessionId, "hash_computed", "Hash calculado", fileHash, 25);

            var existingSyllabus = syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(courseId);
            // Si el registro existente está eliminado lógicamente, no debe activar el
            // atajo de "sin cambios": hay que continuar el flujo normal para que se
            // resucite (deleted=false) aunque el contenido del archivo sea idéntico.
            if (existingSyllabus.isPresent() && !existingSyllabus.get().isDeleted()
                    && fileHash.equals(existingSyllabus.get().getCurrentHash())) {
                eventEmitter.emit(sessionId, "completed", "Sin cambios detectados", "", 100);
                eventEmitter.complete(sessionId);
                SyllabusEntity existing = existingSyllabus.get();
                return new SyllabusResponse(existing.getId(), existing.getCourse().getId(),
                        existing.getCourse().getName(), existing.getCourse().getCode(),
                        existing.getFileUrl(), syllabusFile.getSize(), existing.getCurrentHash(),
                        existing.getStatus(), existing.getCreatedAt(), existing.getFabricTxId());
            }

            eventEmitter.emit(sessionId, "storage_uploading", "Subiendo archivo a Azure Blob Storage...", "", 35);
            String originalFilename = sanitizeFileName(syllabusFile.getOriginalFilename());
            String blobName = String.format("syllabi/course-%d/%s", courseId, originalFilename);
            String fileUrl = uploadToStorage(syllabusFile, fileBytes, blobName);
            eventEmitter.emit(sessionId, "storage_uploaded", "Archivo almacenado", fileUrl, 50);

            SyllabusEntity syllabus;
            if (existingSyllabus.isEmpty()) {
                syllabus = new SyllabusEntity();
                syllabus.setCourse(course);
                syllabus.setCreatedAt(Instant.now());
                syllabus.setCurrentVersion(0);
                syllabus.setLastChainHash("0000000000000000000000000000000000000000000000000000000000000000");
            } else {
                syllabus = existingSyllabus.get();
            }
            syllabus.setFileUrl(fileUrl);
            syllabus.setFileSize(syllabusFile.getSize());
            syllabus.setCurrentHash(fileHash);
            syllabus.setCurrentVersion(syllabus.getCurrentVersion() + 1);
            syllabus.setStatus(action);
            syllabus.setUpdatedAt(Instant.now());
            // Re-subir un sílabo a un curso cuyo registro previo fue eliminado
            // lógicamente (HU0010) debe revertir esa eliminación; de lo contrario
            // findByDeletedFalse() seguiría excluyéndolo aunque la subida sea exitosa.
            syllabus.setDeleted(false);
            syllabus.setDeletedAt(null);
            syllabus.setDeletedBy(null);

            eventEmitter.emit(sessionId, "fabric_connecting", "Conectando con Hyperledger Fabric...", "", 60);
            String txId;
            try {
                eventEmitter.emit(sessionId, "fabric_submitting", "Enviando transacción a Fabric...", "", 70);
                txId = blockchainService.registerSyllabusInFabric(
                        String.valueOf(courseId), fileHash, userEmail, action,
                        syllabusFile.getOriginalFilename(), syllabusFile.getContentType(),
                        syllabusFile.getSize(), userEmail, getInstitutionNameFromUser(userEmail));
                eventEmitter.emit(sessionId, "fabric_confirmed", "Transacción confirmada", txId, 85);
            } catch (BlockchainException e) {
                eventEmitter.emitError(sessionId, "Error en Fabric: " + e.getMessage());
                throw new BlockchainException(
                        "Blockchain registró error: " + e.getMessage() +
                                ". Upload revertido (MinIO podría mantener copia huérfana).", e);
            }

            eventEmitter.emit(sessionId, "db_saving", "Guardando en base de datos...", "", 92);
            syllabus.setFabricTxId(txId);
            SyllabusEntity saved = syllabusRepo.save(syllabus);
            log.info("GUARDADO: syllabusId={}, txId={}", saved.getId(), txId);

            // Registrar la versión en el historial (si el servicio está disponible)
            if (versionService != null) {
                versionService.recordVersion(saved, saved.getCurrentVersion(), action, userEmail,
                        "Nueva versión subida", fileUrl, fileHash, txId);
            }

            eventEmitter.emit(sessionId, "completed", "Sílabo registrado en blockchain", txId, 100);
            eventEmitter.complete(sessionId);

            return new SyllabusResponse(saved.getId(), saved.getCourse().getId(),
                    saved.getCourse().getName(), saved.getCourse().getCode(),
                    saved.getFileUrl(), syllabusFile.getSize(), saved.getCurrentHash(),
                    saved.getStatus(), saved.getCreatedAt(), saved.getFabricTxId());

        } catch (BlockchainException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("ERROR INESPERADO: {}", e.getMessage(), e);
            eventEmitter.emitError(sessionId, "Error: " + e.getMessage());
            throw new RuntimeException("Error inesperado al subir sílabo: " + e.getMessage(), e);
        }
    }

    private void validateInput(Long courseId, MultipartFile file) {
        if (courseId == null || courseId <= 0)
            throw new IllegalArgumentException("courseId debe ser un número positivo");
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Archivo vacío o nulo");
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
            throw new IllegalArgumentException("Nombre de archivo vacío");
        if (file.getSize() > 50 * 1024 * 1024)
            throw new IllegalArgumentException("Archivo demasiado grande (máx 50 MB)");
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error al leer archivo: " + e.getMessage(), e);
        }
    }

    private String uploadToStorage(MultipartFile file, byte[] fileBytes, String blobName) {
        try {
            return azureBlobStorageService.uploadBytes(fileBytes, file.getOriginalFilename(),
                    blobName, file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Error al subir a Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "syllabus.pdf";
        return fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            return "system@siladocs.com";
        return auth.getName();
    }

    private String getInstitutionNameFromUser(String userEmail) {
        try {
            var user = userRepo.findByEmail(userEmail);
            if (user.isPresent() && user.get().getInstitutionId() != null)
                return "Institución-" + user.get().getInstitutionId();
        } catch (Exception e) {
            log.warn("No se pudo obtener institución para {}: {}", userEmail, e.getMessage());
        }
        return "Institución desconocida";
    }

    @Transactional(readOnly = true)
    public List<SyllabusResponse> getAllSyllabi() {
        log.info("[SYLLABI DEBUG] Iniciando getAllSyllabi()");

        // Obtener TODOS los sílabos sin eliminar
        List<SyllabusEntity> allSyllabi = syllabusRepo.findByDeletedFalse();
        log.info("[SYLLABI DEBUG] Total de sílabos en BD (deleted=false): {}", allSyllabi.size());

        // Mapear a respuesta
        List<SyllabusResponse> result = allSyllabi.stream()
                .map(s -> new SyllabusResponse(s.getId(), s.getCourse().getId(),
                        s.getCourse().getName(), s.getCourse().getCode(),
                        s.getFileUrl(), s.getFileSize(), s.getCurrentHash(), s.getStatus(),
                        s.getCreatedAt(), s.getFabricTxId()))
                .collect(Collectors.toList());

        log.info("[SYLLABI DEBUG] Retornando {} sílabos", result.size());
        for (int i = 0; i < result.size(); i++) {
            log.debug("[SYLLABI DEBUG] Syllabus[{}]: id={}, courseId={}, status={}",
                    i, result.get(i).id(), result.get(i).courseId(), result.get(i).status());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<SyllabusResponse> getAllSyllabiForAudit() {
        // Para auditoría: retorna TODOS los sílabos incluyendo eliminados.
        // Permite verificar integridad blockchain de sílabos ya eliminados.
        return syllabusRepo.findAll().stream()
                .map(s -> new SyllabusResponse(s.getId(), s.getCourse().getId(),
                        s.getCourse().getName(), s.getCourse().getCode(),
                        s.getFileUrl(), s.getFileSize(), s.getCurrentHash(), s.getStatus(),
                        s.getCreatedAt(), s.getFabricTxId()))
                .collect(Collectors.toList());
    }

    /**
     * Eliminación lógica (HU0010). Mantiene el archivo, el hash y el historial
     * de versiones intactos para que la trazabilidad en blockchain siga siendo
     * auditable; solo deja de aparecer en los listados activos.
     */
    @Transactional
    public void softDeleteSyllabus(Long id, String deletedByEmail) {
        SyllabusEntity s = syllabusRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sílabo no encontrado: " + id));
        if (s.isDeleted()) {
            throw new IllegalStateException("El sílabo ya fue eliminado anteriormente");
        }
        s.setDeleted(true);
        s.setDeletedAt(Instant.now());
        s.setDeletedBy(deletedByEmail);
        syllabusRepo.save(s);
        log.info("Sílabo {} eliminado (soft delete) por {}", id, deletedByEmail);
    }

    @Transactional(readOnly = true)
    public SyllabusResponse getSyllabusById(Long id) {
        SyllabusEntity s = syllabusRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sílabo no encontrado: " + id));
        return new SyllabusResponse(s.getId(), s.getCourse().getId(),
                s.getCourse().getName(), s.getCourse().getCode(),
                s.getFileUrl(), s.getFileSize(), s.getCurrentHash(), s.getStatus(),
                s.getCreatedAt(), s.getFabricTxId());
    }

    @Transactional
    public SyllabusResponse approveSyllabus(Long id, String approverEmail) {
        SyllabusEntity s = syllabusRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sílabo no encontrado: " + id));
        s.setStatus("validated");
        s.setUpdatedAt(Instant.now());
        SyllabusEntity saved = syllabusRepo.save(s);
        log.info("Sílabo {} aprobado por {}", id, approverEmail);
        return new SyllabusResponse(saved.getId(), saved.getCourse().getId(),
                saved.getCourse().getName(), saved.getCourse().getCode(),
                saved.getFileUrl(), saved.getFileSize(), saved.getCurrentHash(), saved.getStatus(),
                saved.getCreatedAt(), saved.getFabricTxId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyIntegrity(Long id) {
        SyllabusEntity s = syllabusRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sílabo no encontrado: " + id));
        boolean integrityValid = s.getCurrentHash() != null && !s.getCurrentHash().isBlank()
                && s.getFabricTxId() != null && !s.getFabricTxId().isBlank();
        return Map.of(
                "syllabusId", id,
                "storedHash", s.getCurrentHash() != null ? s.getCurrentHash() : "",
                "fabricTxId", s.getFabricTxId() != null ? s.getFabricTxId() : "",
                "integrityValid", integrityValid,
                "status", s.getStatus()
        );
    }
}
