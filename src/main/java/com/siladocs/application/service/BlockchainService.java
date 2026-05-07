package com.siladocs.application.service;

import com.siladocs.application.dto.BlockchainFabricRequestDto;
import com.siladocs.application.dto.BlockchainFabricResponseDto;
import com.siladocs.application.exception.BlockchainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    private final RestTemplate fabricRestTemplate;

    @Value("${blockchain.fabric.mock.enabled:false}")
    private boolean mockEnabled;

    @Value("${blockchain.fabric.api.url:http://localhost:8000}")
    private String fabricApiUrl;

    public BlockchainService(@Qualifier("fabricRestTemplate") RestTemplate fabricRestTemplate) {
        this.fabricRestTemplate = fabricRestTemplate;
    }

    public String registerSyllabusInFabric(String courseId, String fileHash, String issuerEmail, String action,
            String fileName, String fileType, Long fileSize, String uploaderEmail, String institutionName) {

        if (mockEnabled) {
            String mockTxId = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
            log.warn("MOCK MODE: txId={}", mockTxId);
            return mockTxId;
        }

        validateInput(courseId, fileHash, issuerEmail, action);

        String docID = "doc-" + courseId + "-" + System.currentTimeMillis();
        String timestamp = LocalDateTime.now(ZoneId.of("UTC")).format(timestampFormatter);

        BlockchainFabricRequestDto payload = BlockchainFabricRequestDto.builder()
                .docID(docID).courseID(courseId).fileName(fileName).fileType(fileType)
                .fileSize(fileSize).fileHash(fileHash).uploaderEmail(uploaderEmail)
                .institutionName(institutionName).action(action).timestamp(timestamp).build();

        log.info("Enviando a Fabric: {}{}", fabricApiUrl, "/registrar-documento");
        log.debug("Payload: {}", payload);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BlockchainFabricRequestDto> request = new HttpEntity<>(payload, headers);

            ResponseEntity<BlockchainFabricResponseDto> response = fabricRestTemplate.postForEntity(
                    fabricApiUrl + "/registrar-documento",
                    request,
                    BlockchainFabricResponseDto.class);

            BlockchainFabricResponseDto body = response.getBody();
            if (body == null) throw new BlockchainException("Respuesta nula de Fabric API");
            if (!body.isSuccessful()) throw new BlockchainException("Fabric rechazó la transacción: " + body.getMessage());

            String txId = body.getTransactionID();
            if (txId == null || txId.isBlank()) throw new BlockchainException("Fabric no devolvió transaction ID");

            log.info("Fabric OK: txId={}", txId);
            return txId;

        } catch (HttpClientErrorException e) {
            log.error("Error 4xx Fabric: {} - {}", e.getStatusCode(), e.getMessage());
            throw new BlockchainException("Error 4xx en Fabric: " + e.getStatusCode(), e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Error 5xx Fabric: {} - {}", e.getStatusCode(), e.getMessage());
            throw new BlockchainException("Error 5xx en Fabric: " + e.getStatusCode(), e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("Timeout/conexión Fabric: {}", e.getMessage());
            throw new BlockchainException("No se pudo conectar con Fabric en " + fabricApiUrl + ": " + e.getMessage(), e);
        } catch (BlockchainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado Fabric: {}", e.getMessage(), e);
            throw new BlockchainException("Error inesperado: " + e.getMessage(), e);
        }
    }

    public boolean isFabricApiAvailable() {
        try {
            ResponseEntity<String> response = fabricRestTemplate.getForEntity(fabricApiUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Fabric no disponible: {}", e.getMessage());
            return false;
        }
    }

    public String getFabricStatus() {
        boolean available = isFabricApiAvailable();
        return "Fabric API: " + (available ? "UP" : "DOWN") + " | URL: " + fabricApiUrl;
    }

    private void validateInput(String courseId, String fileHash, String issuerEmail, String action) {
        if (courseId == null || courseId.isBlank()) throw new BlockchainException("courseId vacío");
        if (fileHash == null || fileHash.length() != 64) throw new BlockchainException("fileHash inválido");
        if (issuerEmail == null || issuerEmail.isBlank()) throw new BlockchainException("issuerEmail vacío");
        if (action == null || action.isBlank()) throw new BlockchainException("action vacía");
    }
}
