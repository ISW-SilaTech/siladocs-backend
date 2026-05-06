package com.siladocs.application.service;

import com.siladocs.application.dto.BlockchainFabricRequestDto;
import com.siladocs.application.dto.BlockchainFabricResponseDto;
import com.siladocs.application.exception.BlockchainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio de Blockchain refactorizado para usar Hyperledger Fabric.
 *
 * ARQUITECTURA (Clean Architecture):
 * ├── Layer: Application Service (SLL)
 * ├── Dependencies: RestClient (Spring 3.x), BlockchainException
 * └── Purpose: Orquestar comunicación REST con Fabric
 *
 * Responsabilidades:
 * - Construir payload JSON para el registro en Fabric
 * - Realizar solicitud HTTP POST a la API de Fabric
 * - Capturar y validar respuesta
 * - Manejar errores (4xx, 5xx, timeout)
 * - Loguear transacciones con trazabilidad
 *
 * CAMBIOS DE ETHEREUM → FABRIC:
 * - ❌ Eliminado: Web3j, Credentials, Smart Contracts
 * - ✅ Agregado: RestClient, JSON payloads, HTTP error handling
 * - ✅ Simplificado: De escrituras complejas a POST REST simple
 */
@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private static final String FABRIC_REGISTER_ENDPOINT = "/registrar-documento";
    private static final String FABRIC_HEALTH_ENDPOINT = "/health";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    private final RestClient fabricRestClient;

    @Value("${blockchain.fabric.mock.enabled:false}")
    private boolean mockEnabled;

    @Value("${blockchain.fabric.api.url:http://127.0.0.1:8000}")
    private String fabricApiUrl;

    public BlockchainService(@Qualifier("fabricRestClient") RestClient fabricRestClient) {
        this.fabricRestClient = fabricRestClient;
        log.info("BlockchainService inicializado con RestClient para Hyperledger Fabric");
    }

    /**
     * Registra el hash de un sílabo en Hyperledger Fabric.
     *
     * FLUJO PRINCIPAL:
     * 1. Valida parámetros de entrada
     * 2. Construye el payload JSON
     * 3. Realiza POST a /registrar-documento
     * 4. Captura y valida respuesta
     * 5. Maneja errores específicos (4xx, 5xx, timeout)
     * 6. Retorna transactionId si es exitoso
     *
     * @param courseId    ID del curso (string)
     * @param fileHash    SHA-256 del archivo (hexadecimal)
     * @param issuerEmail Email del usuario (issuer de la transacción)
     * @param action      Acción (create, update, etc.)
     * @return Transaction ID de Fabric
     * @throws BlockchainException si falla por cualquier razón
     */
    public String registerSyllabusInFabric(String courseId, String fileHash, String issuerEmail, String action,
            String fileName, String fileType, Long fileSize, String uploaderEmail, String institutionName) {

        if (mockEnabled) {
            String mockTxId = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
            log.warn("⚠️ MOCK MODE: Fabric no activo. txId simulado: {}", mockTxId);
            return mockTxId;
        }

        try {
            // ======= VALIDACIÓN DE ENTRADA =======
            validateInput(courseId, fileHash, issuerEmail, action);

            log.info("📋 Preparando registro en Fabric: courseId={}, hash_prefix={}, issuer={}, action={}",
                    courseId, fileHash.substring(0, 8), issuerEmail, action);

            // ======= CONSTRUIR PAYLOAD =======
            String docID = "doc-" + courseId + "-" + System.currentTimeMillis();
            String timestamp = LocalDateTime.now(ZoneId.of("UTC")).format(timestampFormatter);

            BlockchainFabricRequestDto payload = BlockchainFabricRequestDto.builder()
                    .docID(docID)
                    .courseID(courseId)
                    .fileName(fileName)
                    .fileType(fileType)
                    .fileSize(fileSize)
                    .fileHash(fileHash)
                    .uploaderEmail(uploaderEmail)
                    .institutionName(institutionName)
                    .action(action)
                    .timestamp(timestamp)
                    .build();

            log.debug("✉️ Payload JSON construido: {}", payload);

            // ======= REALIZAR POST =======
            log.info("🔗 Enviando solicitud POST a Fabric: {}{}", getFabricUrl(), FABRIC_REGISTER_ENDPOINT);

            ResponseEntity<BlockchainFabricResponseDto> response = fabricRestClient.post()
                    .uri(FABRIC_REGISTER_ENDPOINT)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, httpResponse) -> {
                        handleFourXxError(courseId, httpResponse);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, httpResponse) -> {
                        handleFiveXxError(courseId, httpResponse);
                    })
                    .toEntity(BlockchainFabricResponseDto.class);

            // ======= VALIDAR RESPUESTA =======
            BlockchainFabricResponseDto fabricResponse = response.getBody();
            if (fabricResponse == null) {
                log.error("❌ Respuesta nula de Fabric API");
                throw new BlockchainException("Respuesta nula de Fabric API");
            }

            if (!fabricResponse.isSuccessful()) {
                log.error("❌ Fabric rechazó la transacción: success={}, message={}",
                        fabricResponse.isSuccessful(), fabricResponse.getMessage());
                throw new BlockchainException(
                        "Fabric rechazó la transacción: " + fabricResponse.getMessage());
            }

            String transactionID = fabricResponse.getTransactionID();
            if (transactionID == null || transactionID.isBlank()) {
                log.error("❌ Fabric no devolvió transaction ID");
                throw new BlockchainException("Fabric no devolvió transaction ID");
            }

            log.info("✅ Sílabo registrado exitosamente en Fabric: courseId={}, transactionID={}, message={}",
                    courseId, transactionID, fabricResponse.getMessage());

            return transactionID;

        } catch (HttpClientErrorException e) {
            // 4xx errors
            log.error("❌ Error 4xx en Fabric (courseId={}): {} - {}", courseId, e.getStatusCode(), e.getMessage());
            throw new BlockchainException(
                    "Error 4xx en Fabric: " + e.getStatusCode() + " - " + e.getMessage(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            // 5xx errors
            log.error("❌ Error 5xx en Fabric (courseId={}): {} - {}", courseId, e.getStatusCode(), e.getMessage());
            throw new BlockchainException(
                    "Error 5xx en Fabric: " + e.getStatusCode(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());

        } catch (RestClientException e) {
            // Timeout, conexión rechazada, DNS error, etc.
            log.error("❌ Error de conexión con Fabric (courseId={}): {}", courseId, e.getMessage(), e);
            throw new BlockchainException(
                    "No se pudo conectar con Fabric. ¿El middleware está activo en " + getFabricUrl() + "?",
                    e);

        } catch (BlockchainException e) {
            // Re-lanzar excepciones personalizadas
            throw e;

        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            log.error("❌ Error inesperado (courseId={}): {}", courseId, e.getMessage(), e);
            throw new BlockchainException(
                    "Error inesperado en BlockchainService: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Verifica la conectividad con la API de Hyperledger Fabric.
     *
     * Útil para:
     * - Health checks
     * - Debugging
     * - Readiness probes (Kubernetes)
     *
     * @return true si la API está disponible, false en caso contrario
     */
    public boolean isFabricApiAvailable() {
        try {
            log.debug("🔍 Verificando disponibilidad de Fabric API...");

            ResponseEntity<String> response = fabricRestClient.get()
                    .uri(FABRIC_HEALTH_ENDPOINT)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Fabric API disponible: {}{}", getFabricUrl(), FABRIC_HEALTH_ENDPOINT);
                return true;
            }

            log.warn("⚠️ Fabric API respondió con status {} ", response.getStatusCode());
            return false;

        } catch (RestClientException e) {
            log.warn("❌ Fabric API no disponible: {} ({})", getFabricUrl(), e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el estado detallado de Fabric para debugging.
     *
     * @return String con información del estado
     */
    public String getFabricStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Fabric API Status Report\n");
        status.append("=".repeat(50)).append("\n");

        try {
            boolean available = isFabricApiAvailable();
            status.append("API Available: ").append(available ? "✅ YES" : "❌ NO").append("\n");
            status.append("URL: ").append(getFabricUrl()).append("\n");
            status.append("Endpoint: ").append(FABRIC_REGISTER_ENDPOINT).append("\n");
            status.append("Health: ").append(FABRIC_HEALTH_ENDPOINT).append("\n");
        } catch (Exception e) {
            status.append("Error getting status: ").append(e.getMessage()).append("\n");
        }

        return status.toString();
    }

    // ============================================================================
    // MÉTODOS PRIVADOS (HELPERS)
    // ============================================================================

    /**
     * Valida que los parámetros de entrada sean válidos.
     *
     * @throws BlockchainException si algún parámetro es inválido
     */
    private void validateInput(String courseId, String fileHash, String issuerEmail, String action) {
        if (courseId == null || courseId.isBlank()) {
            throw new BlockchainException("courseId no puede estar vacío");
        }
        if (fileHash == null || fileHash.isBlank()) {
            throw new BlockchainException("fileHash no puede estar vacío");
        }
        if (fileHash.length() != 64) {
            throw new BlockchainException("fileHash debe ser SHA-256 (64 caracteres hexadecimales)");
        }
        if (issuerEmail == null || issuerEmail.isBlank()) {
            throw new BlockchainException("issuerEmail no puede estar vacío");
        }
        if (action == null || action.isBlank()) {
            throw new BlockchainException("action no puede estar vacía");
        }
    }

    /**
     * Maneja errores 4xx (Bad Request, 404, etc.).
     */
    private void handleFourXxError(String courseId, org.springframework.http.client.ClientHttpResponse httpResponse) {
        try {
            String errorBody = new String(httpResponse.getBody().readAllBytes());
            log.error("❌ Error 4xx en Fabric (courseId={}): status={}, body={}",
                    courseId, httpResponse.getStatusCode(), errorBody);
            throw new BlockchainException(
                    "Error 4xx en Fabric",
                    httpResponse.getStatusCode().value(),
                    errorBody);
        } catch (Exception e) {
            log.error("Error leyendo respuesta 4xx: {}", e.getMessage());
            throw new BlockchainException("Error 4xx en Fabric", e);
        }
    }

    /**
     * Maneja errores 5xx (Server Error, etc.).
     */
    private void handleFiveXxError(String courseId, org.springframework.http.client.ClientHttpResponse httpResponse) {
        try {
            String errorBody = new String(httpResponse.getBody().readAllBytes());
            log.error("❌ Error 5xx en Fabric (courseId={}): status={}, body={}",
                    courseId, httpResponse.getStatusCode(), errorBody);
            throw new BlockchainException(
                    "Error 5xx en Fabric",
                    httpResponse.getStatusCode().value(),
                    errorBody);
        } catch (Exception e) {
            log.error("Error leyendo respuesta 5xx: {}", e.getMessage());
            throw new BlockchainException("Error 5xx en Fabric", e);
        }
    }

    /**
     * Obtiene la URL base de Fabric (para logging).
     */
    private String getFabricUrl() {
        return fabricApiUrl;
    }
}
