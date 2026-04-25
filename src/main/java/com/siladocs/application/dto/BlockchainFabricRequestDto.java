package com.siladocs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el payload enviado a la API REST de Hyperledger Fabric (middleware Python).
 *
 * La API espera:
 * {
 *   "curso_id": "String",
 *   "file_hash": "String (SHA-256)",
 *   "issuer": "String (Email o Nombre del admin)",
 *   "date": "String (YYYY-MM-DD)"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainFabricRequestDto {

    /**
     * ID del curso (para relacionar la transacción con el syllabus)
     */
    private String curso_id;

    /**
     * Hash SHA-256 del archivo del sílabo
     */
    private String file_hash;

    /**
     * Email o nombre del usuario que registra (admin)
     */
    private String issuer;

    /**
     * Fecha de registro en formato YYYY-MM-DD
     */
    private String date;

        /**
     * Nombre del archivo del documento
     */
    private String file_name;

    /**
     * Tipo MIME del documento
     */
    private String file_type;

    /**
     * Tamaño del archivo en bytes
     */
    private Long file_size;

    /**
     * Email del usuario que sube el documento
     */
    private String uploader_email;

    /**
     * Nombre de la institución (desde AuthController)
     */
    private String institution_name;

    /**
     * Acción: create, update, delete
     */
    private String action;
}
