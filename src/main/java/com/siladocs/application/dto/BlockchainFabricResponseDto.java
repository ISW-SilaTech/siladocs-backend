package com.siladocs.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de la API REST de Hyperledger Fabric (middleware Python).
 *
 * The middleware returns:
 * {
 *   "success": boolean,
 *   "transactionID": "String (transaction ID en Fabric)",
 *   "message": "String (mensaje descriptivo)",
 *   "data": { ... document metadata ... }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainFabricResponseDto {

    /**
     * Indica si la transacción fue exitosa
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * ID de la transacción en Hyperledger Fabric
     */
    @JsonProperty("transactionID")
    private String transactionID;

    /**
     * Mensaje descriptivo de la respuesta
     */
    @JsonProperty("message")
    private String message;

    /**
     * Metadata del documento registrado
     */
    @JsonProperty("data")
    private java.util.Map<String, Object> data;

    /**
     * Verifica si la transacción fue exitosa.
     *
     * @return true si success es true
     */
    public boolean isSuccessful() {
        return success;
    }

    /**
     * Para retrocompatibilidad: getter para txId que retorna transactionID
     */
    public String getTxId() {
        return transactionID;
    }
}
