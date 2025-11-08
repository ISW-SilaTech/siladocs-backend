package com.siladocs.application.service;

import com.siladocs.domain.model.User;
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
    private final SyllabusHistoryLogRepository historyRepo;

    public SyllabusService(SyllabusJpaRepository syllabusRepo,
                           CourseJpaRepository courseRepo,
                           UserRepository userRepo,
                           BlockchainService blockchainService,
                           SyllabusHistoryLogRepository historyRepo) {
        this.syllabusRepo = syllabusRepo;
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
        this.blockchainService = blockchainService;
        this.historyRepo = historyRepo;
    }

    @Transactional
    public void uploadSyllabus(Long courseId, String fileContent, String fileUrl, String action) {

        String userEmail = getAuthenticatedUserEmail();
        CourseEntity course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        String fileHash = DigestUtils.sha256Hex(fileContent);

        SyllabusEntity syllabus = syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(courseId)
                .orElse(new SyllabusEntity());

        if (fileHash.equals(syllabus.getCurrentHash())) {
            log.info("Hash de sílabo sin cambios para el curso {}. No se requiere actualización.", courseId);
            return;
        }

        // ⬇️ 🔹 --- CORRECCIÓN AQUÍ --- 🔹 ⬇️
        // Si es un sílabo nuevo, inicializa
        if (syllabus.getId() == null) {
            syllabus.setCourse(course);
            syllabus.setCreatedAt(Instant.now());
            syllabus.setCurrentVersion(0); // Se incrementará a 1
            // 🔹 INICIALIZA EL HASH DE LA CADENA
            syllabus.setLastChainHash("0000000000000000000000000000000000000000000000000000000000000000");
        }
        // ⬆️ 🔹 --- FIN DE LA CORRECCIÓN --- 🔹 ⬆️

        // 1. Guardar en PostgreSQL
        syllabus.setFileUrl(fileUrl);
        syllabus.setCurrentHash(fileHash);
        syllabus.setCurrentVersion(syllabus.getCurrentVersion() + 1);
        syllabus.setStatus(action);
        syllabus.setUpdatedAt(Instant.now());

        SyllabusEntity savedSyllabus = syllabusRepo.save(syllabus);
        log.info("Sílabo (versión {}) guardado en SQL para curso ID {}", savedSyllabus.getCurrentVersion(), courseId);

        // 2. Registrar en Blockchain
        try {
            String txHash = blockchainService.registerSyllabusVersion(
                    savedSyllabus.getId(), // ⬅️ ID del SÍLABO
                    fileHash,
                    userEmail,
                    action
            );
            log.info("Sílabo ID {} (v{}) registrado en Blockchain. TxHash: {}",
                    savedSyllabus.getId(), savedSyllabus.getCurrentVersion(), txHash);

        } catch (Exception e) {
            log.error("¡FALLO CRÍTICO! No se pudo registrar en Blockchain: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar en Blockchain. La subida del sílabo fue revertida.", e);
        }
    }

    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("No se encontró usuario autenticado. Usando 'system@siladocs.com' para el log de blockchain.");
            return "system@siladocs.com";
        }
        return authentication.getName();
    }
}