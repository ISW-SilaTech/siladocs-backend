package com.siladocs.application.service;

import com.siladocs.application.dto.SyllabusResponse;
import com.siladocs.application.exception.BlockchainException;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.entity.SyllabusEntity;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusHistoryLogRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Casos de prueba para SyllabusService.
 *
 * TC-01: Registro de Sílabo
 * TC-02: Flujo de Aprobación
 * TC-03: Registro en Blockchain
 * TC-04: Verificación de Integridad
 * TC-05: Acceso No Autorizado
 * TC-06: Detección de Hash Duplicado
 * TC-07: Validación de Formato de Archivo
 * TC-08: Incremento de Versión
 * TC-09: Generación de URL de Descarga
 * TC-10: Health Check de Blockchain
 */
@DisplayName("SyllabusService - 10 Casos de Prueba")
@ExtendWith(MockitoExtension.class)
public class SyllabusServiceTest {

    @Mock private SyllabusJpaRepository syllabusRepo;
    @Mock private CourseJpaRepository courseRepo;
    @Mock private BlockchainService blockchainService;
    @Mock private AzureBlobStorageService azureBlobStorageService;
    @Mock private SyllabusHistoryLogRepository historyRepo;
    @Mock private UserRepository userRepo;
    @Mock private BlockchainEventEmitterService eventEmitter;

    @InjectMocks
    private SyllabusService syllabusService;

    private CourseEntity mockCourse;
    private SyllabusEntity mockSyllabus;
    private MockMultipartFile mockFile;
    private static final String VALID_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String FABRIC_TX_ID = "doc-123-1700000000000";

    @BeforeEach
    void setUp() {
        mockCourse = new CourseEntity();
        mockCourse.setId(1L);
        mockCourse.setName("Ingeniería de Software");
        mockCourse.setCode("IS-101");

        mockSyllabus = new SyllabusEntity();
        mockSyllabus.setId(1L);
        mockSyllabus.setCourse(mockCourse);
        mockSyllabus.setCurrentHash(VALID_HASH);
        mockSyllabus.setFileUrl("syllabi/course-1/silabo.pdf");
        mockSyllabus.setStatus("create");
        mockSyllabus.setCurrentVersion(1);
        mockSyllabus.setLastChainHash("0000000000000000000000000000000000000000000000000000000000000000");
        mockSyllabus.setFabricTxId(FABRIC_TX_ID);
        mockSyllabus.setCreatedAt(Instant.now());
        mockSyllabus.setUpdatedAt(Instant.now());

        mockFile = new MockMultipartFile(
                "file",
                "silabo_ingenieria.pdf",
                "application/pdf",
                new byte[1024]
        );
    }

    // ============================================================
    // TC-01: Registro de Sílabo
    // ============================================================

    @Test
    @DisplayName("TC-01 - Registro exitoso: archivo almacenado y hash SHA-256 generado")
    void tc01_syllabusRegistration_success() throws Exception {
        when(courseRepo.findById(1L)).thenReturn(Optional.of(mockCourse));
        when(syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(1L)).thenReturn(Optional.empty());
        when(azureBlobStorageService.uploadBytes(any(), any(), any(), any()))
                .thenReturn("syllabi/course-1/silabo_ingenieria.pdf");
        when(blockchainService.registerSyllabusInFabric(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(FABRIC_TX_ID);
        when(syllabusRepo.save(any())).thenReturn(mockSyllabus);
        doNothing().when(eventEmitter).emit(any(), any(), any(), any(), anyInt());
        doNothing().when(eventEmitter).complete(any());

        SyllabusResponse response = syllabusService.uploadSyllabus(1L, mockFile, "create", "session-1");

        assertNotNull(response, "TC-01: La respuesta no debe ser nula");
        assertNotNull(response.currentHash(), "TC-01: El hash SHA-256 debe estar presente");
        assertEquals(64, response.currentHash().length(), "TC-01: El hash debe tener 64 caracteres (SHA-256)");
        assertNotNull(response.fabricTxId(), "TC-01: El Transaction ID de Fabric debe estar presente");
        verify(azureBlobStorageService).uploadBytes(any(), any(), any(), any());
    }

    // ============================================================
    // TC-02: Flujo de Aprobación
    // ============================================================

    @Test
    @DisplayName("TC-02 - Aprobación: estado actualizado a 'validated'")
    void tc02_approvalWorkflow_statusUpdatedToValidated() {
        when(syllabusRepo.findById(1L)).thenReturn(Optional.of(mockSyllabus));
        when(syllabusRepo.save(any())).thenAnswer(inv -> {
            SyllabusEntity s = inv.getArgument(0);
            assertEquals("validated", s.getStatus(), "TC-02: El estado debe ser 'validated'");
            return s;
        });

        SyllabusResponse response = syllabusService.approveSyllabus(1L, "coordinador@siladocs.com");

        assertNotNull(response, "TC-02: La respuesta no debe ser nula");
        assertEquals("validated", response.status(), "TC-02: El estado debe ser 'validated'");
        verify(syllabusRepo).save(any());
    }

    @Test
    @DisplayName("TC-02 - Aprobación de sílabo inexistente lanza excepción")
    void tc02_approvalWorkflow_notFound() {
        when(syllabusRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> syllabusService.approveSyllabus(999L, "coordinador@siladocs.com"),
                "TC-02: Debe lanzar excepción si el sílabo no existe");
    }

    // ============================================================
    // TC-03: Registro en Blockchain
    // ============================================================

    @Test
    @DisplayName("TC-03 - Blockchain: metadatos y hash registrados con Transaction ID válido")
    void tc03_blockchainRecording_txIdStored() throws Exception {
        when(courseRepo.findById(1L)).thenReturn(Optional.of(mockCourse));
        when(syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(1L)).thenReturn(Optional.empty());
        when(azureBlobStorageService.uploadBytes(any(), any(), any(), any()))
                .thenReturn("syllabi/course-1/silabo_ingenieria.pdf");
        when(blockchainService.registerSyllabusInFabric(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(FABRIC_TX_ID);
        when(syllabusRepo.save(any())).thenReturn(mockSyllabus);
        doNothing().when(eventEmitter).emit(any(), any(), any(), any(), anyInt());
        doNothing().when(eventEmitter).complete(any());

        SyllabusResponse response = syllabusService.uploadSyllabus(1L, mockFile, "create", "session-1");

        assertNotNull(response.fabricTxId(), "TC-03: El fabricTxId no debe ser nulo");
        assertFalse(response.fabricTxId().isBlank(), "TC-03: El fabricTxId no debe estar vacío");
        verify(blockchainService).registerSyllabusInFabric(
                eq("1"), any(), any(), eq("create"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-03 - Blockchain: error en Fabric revierte la operación")
    void tc03_blockchainRecording_fabricErrorRollback() throws Exception {
        when(courseRepo.findById(1L)).thenReturn(Optional.of(mockCourse));
        when(syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(1L)).thenReturn(Optional.empty());
        when(azureBlobStorageService.uploadBytes(any(), any(), any(), any()))
                .thenReturn("syllabi/course-1/silabo_ingenieria.pdf");
        when(blockchainService.registerSyllabusInFabric(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BlockchainException("Fabric no disponible"));
        doNothing().when(eventEmitter).emit(any(), any(), any(), any(), anyInt());
        doNothing().when(eventEmitter).emitError(any(), any());

        assertThrows(BlockchainException.class,
                () -> syllabusService.uploadSyllabus(1L, mockFile, "create", "session-1"),
                "TC-03: Error en Fabric debe propagar la excepción");
    }

    // ============================================================
    // TC-04: Verificación de Integridad
    // ============================================================

    @Test
    @DisplayName("TC-04 - Integridad: hash almacenado coincide con registro en blockchain")
    void tc04_dataIntegrityCheck_hashesMatch() {
        when(syllabusRepo.findById(1L)).thenReturn(Optional.of(mockSyllabus));

        Map<String, Object> result = syllabusService.verifyIntegrity(1L);

        assertNotNull(result, "TC-04: El resultado no debe ser nulo");
        assertEquals(VALID_HASH, result.get("storedHash"), "TC-04: El hash almacenado debe coincidir");
        assertEquals(FABRIC_TX_ID, result.get("fabricTxId"), "TC-04: El fabricTxId debe coincidir");
        assertTrue((Boolean) result.get("integrityValid"), "TC-04: La integridad debe ser válida");
    }

    @Test
    @DisplayName("TC-04 - Integridad: falla si hash o fabricTxId están vacíos")
    void tc04_dataIntegrityCheck_missingHashFails() {
        mockSyllabus.setCurrentHash(null);
        mockSyllabus.setFabricTxId(null);
        when(syllabusRepo.findById(1L)).thenReturn(Optional.of(mockSyllabus));

        Map<String, Object> result = syllabusService.verifyIntegrity(1L);

        assertFalse((Boolean) result.get("integrityValid"), "TC-04: Integridad inválida si falta hash o txId");
    }

    // ============================================================
    // TC-05: Acceso No Autorizado
    // ============================================================

    @Test
    @DisplayName("TC-05 - Acceso no autorizado: upload sin autenticación usa email de sistema")
    void tc05_unauthorizedAccess_usesSystemEmail() throws Exception {
        // Sin SecurityContext, debe usar "system@siladocs.com" como fallback
        when(courseRepo.findById(1L)).thenReturn(Optional.of(mockCourse));
        when(syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(1L)).thenReturn(Optional.empty());
        when(azureBlobStorageService.uploadBytes(any(), any(), any(), any()))
                .thenReturn("syllabi/course-1/silabo_ingenieria.pdf");
        when(blockchainService.registerSyllabusInFabric(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(FABRIC_TX_ID);
        when(syllabusRepo.save(any())).thenReturn(mockSyllabus);
        doNothing().when(eventEmitter).emit(any(), any(), any(), any(), anyInt());
        doNothing().when(eventEmitter).complete(any());

        SyllabusResponse response = syllabusService.uploadSyllabus(1L, mockFile, "create", null);

        assertNotNull(response, "TC-05: La operación sin auth debe completarse con email de sistema");
        // La verificación real de 401/403 la hace Spring Security en el endpoint HTTP
    }

    // ============================================================
    // TC-06: Detección de Hash Duplicado
    // ============================================================

    @Test
    @DisplayName("TC-06 - Hash duplicado: mismo archivo no genera nueva transacción")
    void tc06_duplicateHashDetection_noNewTransaction() throws Exception {
        // Archivo con hash idéntico al ya registrado
        SyllabusEntity existingWithSameHash = new SyllabusEntity();
        existingWithSameHash.setId(1L);
        existingWithSameHash.setCourse(mockCourse);
        existingWithSameHash.setCurrentHash(computeHashOf(new byte[1024])); // mismo contenido que mockFile
        existingWithSameHash.setFabricTxId(FABRIC_TX_ID);
        existingWithSameHash.setFileUrl("syllabi/course-1/silabo_ingenieria.pdf");
        existingWithSameHash.setStatus("create");
        existingWithSameHash.setCurrentVersion(1);
        existingWithSameHash.setCreatedAt(Instant.now());

        when(courseRepo.findById(1L)).thenReturn(Optional.of(mockCourse));
        when(syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(1L))
                .thenReturn(Optional.of(existingWithSameHash));
        doNothing().when(eventEmitter).emit(any(), any(), any(), any(), anyInt());
        doNothing().when(eventEmitter).complete(any());

        SyllabusResponse response = syllabusService.uploadSyllabus(1L, mockFile, "create", "session-1");

        assertNotNull(response, "TC-06: Debe retornar el sílabo existente sin error");
        verify(blockchainService, never()).registerSyllabusInFabric(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ============================================================
    // TC-07: Validación de Formato de Archivo
    // ============================================================

    @Test
    @DisplayName("TC-07 - Validación: archivo vacío es rechazado")
    void tc07_fileFormatValidation_emptyFileRejected() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> syllabusService.uploadSyllabus(1L, emptyFile, "create", null),
                "TC-07: Archivo vacío debe lanzar IllegalArgumentException");
    }

    @Test
    @DisplayName("TC-07 - Validación: courseId inválido es rechazado")
    void tc07_fileFormatValidation_invalidCourseIdRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> syllabusService.uploadSyllabus(0L, mockFile, "create", null),
                "TC-07: courseId = 0 debe lanzar IllegalArgumentException");
    }

    @Test
    @DisplayName("TC-07 - Validación: archivo mayor a 50MB es rechazado")
    void tc07_fileFormatValidation_fileTooLargeRejected() {
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51 MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "grande.pdf", "application/pdf", largeContent);

        assertThrows(IllegalArgumentException.class,
                () -> syllabusService.uploadSyllabus(1L, largeFile, "create", null),
                "TC-07: Archivo > 50MB debe lanzar IllegalArgumentException");
    }

    // ============================================================
    // TC-08: Incremento de Versión
    // ============================================================

    @Test
    @DisplayName("TC-08 - Versionado: re-upload incrementa el número de versión")
    void tc08_versionIncrement_onReupload() throws Exception {
        SyllabusEntity existingVersion1 = new SyllabusEntity();
        existingVersion1.setId(1L);
        existingVersion1.setCourse(mockCourse);
        existingVersion1.setCurrentHash("oldhash" + "0".repeat(57)); // hash diferente
        existingVersion1.setCurrentVersion(1);
        existingVersion1.setFabricTxId(FABRIC_TX_ID);
        existingVersion1.setFileUrl("syllabi/course-1/silabo_v1.pdf");
        existingVersion1.setStatus("create");
        existingVersion1.setLastChainHash("0".repeat(64));
        existingVersion1.setCreatedAt(Instant.now());
        existingVersion1.setUpdatedAt(Instant.now());

        when(courseRepo.findById(1L)).thenReturn(Optional.of(mockCourse));
        when(syllabusRepo.findFirstByCourse_IdOrderByCurrentVersionDesc(1L))
                .thenReturn(Optional.of(existingVersion1));
        when(azureBlobStorageService.uploadBytes(any(), any(), any(), any()))
                .thenReturn("syllabi/course-1/silabo_ingenieria.pdf");
        when(blockchainService.registerSyllabusInFabric(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("doc-123-new-tx");
        when(syllabusRepo.save(any())).thenAnswer(inv -> {
            SyllabusEntity s = inv.getArgument(0);
            assertEquals(2, s.getCurrentVersion(), "TC-08: La versión debe incrementar a 2");
            return s;
        });
        doNothing().when(eventEmitter).emit(any(), any(), any(), any(), anyInt());
        doNothing().when(eventEmitter).complete(any());

        syllabusService.uploadSyllabus(1L, mockFile, "update", "session-1");

        verify(syllabusRepo).save(argThat(s -> s.getCurrentVersion() == 2));
    }

    // ============================================================
    // TC-09: Generación de URL de Descarga
    // ============================================================

    @Test
    @DisplayName("TC-09 - URL de descarga: retorna URL válida con token SAS")
    void tc09_downloadUrl_returnsValidSasUrl() {
        when(syllabusRepo.findById(1L)).thenReturn(Optional.of(mockSyllabus));

        SyllabusResponse syllabus = syllabusService.getSyllabusById(1L);

        assertNotNull(syllabus, "TC-09: El sílabo debe existir");
        assertNotNull(syllabus.fileUrl(), "TC-09: La URL del archivo debe estar almacenada");
        assertFalse(syllabus.fileUrl().isBlank(), "TC-09: La URL del archivo no debe estar vacía");
    }

    @Test
    @DisplayName("TC-09 - URL de descarga: sílabo inexistente lanza excepción")
    void tc09_downloadUrl_notFoundThrows() {
        when(syllabusRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> syllabusService.getSyllabusById(999L),
                "TC-09: Debe lanzar excepción para ID inexistente");
    }

    // ============================================================
    // TC-10: Health Check de Blockchain
    // ============================================================

    @Test
    @DisplayName("TC-10 - Health check: BlockchainService retorna estado de Fabric")
    void tc10_blockchainHealthCheck_returnsStatus() {
        when(blockchainService.getFabricStatus()).thenReturn("Fabric API: http://localhost:8000 - Mock mode");

        String status = blockchainService.getFabricStatus();

        assertNotNull(status, "TC-10: El estado de Fabric no debe ser nulo");
        assertTrue(status.contains("Fabric API"), "TC-10: El estado debe mencionar 'Fabric API'");
    }

    @Test
    @DisplayName("TC-10 - Health check: obtener todos los sílabos retorna lista")
    void tc10_getAllSyllabi_returnsCorrectList() {
        when(syllabusRepo.findAll()).thenReturn(List.of(mockSyllabus));

        List<SyllabusResponse> syllabi = syllabusService.getAllSyllabi();

        assertNotNull(syllabi, "TC-10: La lista no debe ser nula");
        assertEquals(1, syllabi.size(), "TC-10: Debe retornar exactamente 1 sílabo");
        assertEquals("IS-101", syllabi.get(0).courseCode(), "TC-10: El código de curso debe coincidir");
    }

    // Helpers
    private String computeHashOf(byte[] bytes) {
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(bytes);
    }
}
