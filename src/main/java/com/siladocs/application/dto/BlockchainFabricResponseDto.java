package com.siladocs.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de la API REST de Hyperledger Fabric (middleware Python).
 *
 * La API devuelve:
 * {
 *   "status": "success" | "error",
 *   "txId": "String (transaction ID en Fabric)",
 *   "message": "String (mensaje descriptivo)",
 *   "timestamp": "String (ISO-8601)"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainFabricResponseDto {

    /**
     * Estado de la transacción: "success" o "error"
     */
    @JsonProperty("status")
    private String status;

    /**
     * ID de la transacción en Hyperledger Fabric
     */
    @JsonProperty("txId")
    private String txId;

    /**
     * Mensaje descriptivo de la respuesta
     */
    @JsonProperty("message")
    private String message;

    /**
     * Timestamp en formato ISO-8601
     */
    @JsonProperty("timestamp")
    private String timestamp;

    /**
     * Verifica si la transacción fue exitosa.
     *
     * @return true si status es "success"
     */
    public boolean isSuccessful() {
        return "success".equalsIgnoreCase(status);
    }
}
