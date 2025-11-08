package com.siladocs.application.service;

import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.entity.SyllabusEntity;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusHistoryLogRepository; // 🔹 Importar
import com.siladocs.infrastructure.persistence.jparepository.SyllabusJpaRepository; // 🔹 Importar
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SyllabusService {

    private static final Logger log = LoggerFactory.getLogger(SyllabusService.class);

    private final SyllabusJpaRepository syllabusRepo;
    private final CourseJpaRepository courseRepo;
    private final UserRepository userRepo;
    private final BlockchainService blockchainService;
    // 🔹(Si tienes la tabla de historial SQL, añade el repo aquí)
    // private final SyllabusHistoryLogRepository historyRepo;

    public SyllabusService(SyllabusJpaRepository syllabusRepo,
                           CourseJpaRepository courseRepo,
                           UserRepository userRepo,
                           BlockchainService blockchainService) {
        this.syllabusRepo = syllabusRepo;
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
        this.blockchainService = blockchainService;
    }

    /**
     * Sube un nuevo sílabo (o una nueva versión de uno existente).
     * Esto reemplaza la lógica de "create" y "update".
     */
    @Transactional
    public void uploadSyllabus(Long courseId, String userEmail, String fileContent, String fileUrl, String action) {

        CourseEntity course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        String fileHash = DigestUtils.sha256Hex(fileContent);

        // Busca si ya existe un sílabo para este curso
        SyllabusEntity syllabus = syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(courseId)
                .orElse(new SyllabusEntity()); // Si no existe, crea uno nuevo

        // Si el hash es el mismo, no hacemos nada (el archivo no cambió)
        if (fileHash.equals(syllabus.getCurrentHash())) {
            log.info("Hash de sílabo sin cambios para el curso {}. No se requiere actualización.", courseId);
            return;
        }

        // --- Hay un cambio, guardamos la nueva versión ---

        // Si es un sílabo nuevo, inicializa
        if (syllabus.getId() == null) {
            syllabus.setCourse(course);
            syllabus.setCreatedAt(Instant.now());
            syllabus.setCurrentVersion(0); // Se incrementará a 1
        }

        // 1. Guardar en PostgreSQL
        syllabus.setFileUrl(fileUrl);
        syllabus.setCurrentHash(fileHash);
        syllabus.setCurrentVersion(syllabus.getCurrentVersion() + 1);
        syllabus.setStatus(action); // Ej: "CARGADO", "APROBADO"
        syllabus.setUpdatedAt(Instant.now());
        // El 'last_chain_hash' se actualizará por el trigger (si usamos el trigger SQL)
        // O lo dejamos nulo si solo usamos Ganache.

        SyllabusEntity savedSyllabus = syllabusRepo.save(syllabus);
        log.info("Sílabo (versión {}) guardado en SQL para curso ID {}", savedSyllabus.getCurrentVersion(), courseId);

        // 2. Registrar en Blockchain
        try {
            String txHash = blockchainService.registerSyllabusVersion(
                    savedSyllabus.getId(), // ID del sílabo
                    fileHash,
                    userEmail,
                    action // "CARGADO", "MODIFICADO", "APROBADO"
            );
            log.info("Sílabo ID {} (v{}) registrado en Blockchain. TxHash: {}",
                    savedSyllabus.getId(), savedSyllabus.getCurrentVersion(), txHash);

        } catch (Exception e) {
            log.error("¡FALLO CRÍTICO! No se pudo registrar en Blockchain: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar en Blockchain. La subida del sílabo fue revertida.", e);
        }
    }
}