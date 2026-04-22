package com.siladocs.application.controller;

import com.siladocs.application.service.BlockchainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de Salud (Health Check) para Hyperledger Fabric.
 *
 * Endpoints:
 * - GET /health/fabric       - Health check básico
 * - GET /health/fabric/detail - Información detallada
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Endpoints de salud del sistema")
@RequiredArgsConstructor
public class HealthController {

    private final BlockchainService blockchainService;

    /**
     * Health check simple para Fabric.
     *
     * @return 200 OK si Fabric está disponible, 503 si no
     */
    @GetMapping("/fabric")
    @Operation(summary = "Health check de Fabric", description = "Verifica disponibilidad de la API de Hyperledger Fabric")
    public ResponseEntity<Map<String, Object>> fabricHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());

        if (blockchainService.isFabricApiAvailable()) {
            response.put("status", "UP");
            response.put("service", "Hyperledger Fabric");
            response.put("message", "API disponible");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            response.put("service", "Hyperledger Fabric");
            response.put("message", "No se pudo conectar con la API de Fabric");
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Health check detallado con más información.
     *
     * @return 200 OK con estado detallado
     */
    @GetMapping("/fabric/detail")
    @Operation(summary = "Health check detallado de Fabric", description = "Devuelve información detallada del estado de Fabric")
    public ResponseEntity<Map<String, String>> fabricHealthCheckDetail() {
        Map<String, String> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("service", "Hyperledger Fabric");
        response.put("details", blockchainService.getFabricStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de salud general del sistema (incluyendo Fabric).
     */
    @GetMapping
    @Operation(summary = "Health check general", description = "Verifica el estado general del sistema")
    public ResponseEntity<Map<String, Object>> generalHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", "UP");
        response.put("application", "SilaDocs Backend");
        response.put("fabric_available", blockchainService.isFabricApiAvailable());

        return ResponseEntity.ok(response);
    }
}
