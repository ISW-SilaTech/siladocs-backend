package com.siladocs.application.service;

import com.siladocs.application.dto.SyllabusVersionDto;
import com.siladocs.infrastructure.persistence.entity.SyllabusEntity;
import com.siladocs.infrastructure.persistence.entity.SyllabusVersionHistoryEntity;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusVersionHistoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SyllabusVersionService {

    private final SyllabusVersionHistoryJpaRepository versionHistoryRepo;

    public SyllabusVersionService(SyllabusVersionHistoryJpaRepository versionHistoryRepo) {
        this.versionHistoryRepo = versionHistoryRepo;
    }

    @Transactional
    public void recordVersion(SyllabusEntity syllabus, Integer versionNumber, String status,
                            String uploadedBy, String notes, String fileUrl, String fileHash, String fabricTxId) {
        SyllabusVersionHistoryEntity versionHistory = new SyllabusVersionHistoryEntity();
        versionHistory.setSyllabus(syllabus);
        versionHistory.setVersionNumber(versionNumber);
        versionHistory.setFileUrl(fileUrl);
        versionHistory.setFileHash(fileHash);
        versionHistory.setStatus(status);
        versionHistory.setUploadedBy(uploadedBy);
        versionHistory.setNotes(notes);
        versionHistory.setFabricTxId(fabricTxId);
        versionHistory.setCreatedAt(Instant.now());

        versionHistoryRepo.save(versionHistory);
    }

    @Transactional(readOnly = true)
    public List<SyllabusVersionDto> getSyllabusVersionHistory(Long syllabusId) {
        List<SyllabusVersionHistoryEntity> history = versionHistoryRepo
                .findBySyllabus_IdOrderByVersionNumberDesc(syllabusId);

        return history.stream()
                .map(v -> new SyllabusVersionDto(
                        v.getId(),
                        v.getVersionNumber(),
                        v.getFileUrl(),
                        v.getFileHash(),
                        v.getStatus(),
                        v.getUploadedBy(),
                        v.getCreatedAt(),
                        v.getNotes(),
                        v.getFabricTxId() != null && !v.getFabricTxId().isEmpty(),
                        v.getFabricTxId()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SyllabusVersionDto getSpecificVersion(Long syllabusId, Integer versionNumber) {
        return versionHistoryRepo.findBySyllabus_IdAndVersionNumber(syllabusId, versionNumber)
                .map(v -> new SyllabusVersionDto(
                        v.getId(),
                        v.getVersionNumber(),
                        v.getFileUrl(),
                        v.getFileHash(),
                        v.getStatus(),
                        v.getUploadedBy(),
                        v.getCreatedAt(),
                        v.getNotes(),
                        v.getFabricTxId() != null && !v.getFabricTxId().isEmpty(),
                        v.getFabricTxId()
                ))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Versión " + versionNumber + " no encontrada para sílabo " + syllabusId));
    }
}
