package com.siladocs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el payload enviado a la API REST de Hyperledger Fabric (middleware Python).
 *
 * Matches the middleware API contract at POST /registrar-documento:
 * {
 *   "docID": "String",
 *   "courseID": "String",
 *   "fileName": "String",
 *   "fileType": "String (MIME type)",
 *   "fileSize": "Long",
 *   "fileHash": "String (SHA-256)",
 *   "uploaderEmail": "String",
 *   "institutionName": "String",
 *   "action": "String (create|update|delete)",
 *   "timestamp": "String (ISO-8601)"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainFabricRequestDto {

    @com.fasterxml.jackson.annotation.JsonProperty("docID")
    private String docID;

    @com.fasterxml.jackson.annotation.JsonProperty("courseID")
    private String courseID;

    @com.fasterxml.jackson.annotation.JsonProperty("fileName")
    private String fileName;

    @com.fasterxml.jackson.annotation.JsonProperty("fileType")
    private String fileType;

    @com.fasterxml.jackson.annotation.JsonProperty("fileSize")
    private Long fileSize;

    @com.fasterxml.jackson.annotation.JsonProperty("fileHash")
    private String fileHash;

    @com.fasterxml.jackson.annotation.JsonProperty("uploaderEmail")
    private String uploaderEmail;

    @com.fasterxml.jackson.annotation.JsonProperty("institutionName")
    private String institutionName;

    @com.fasterxml.jackson.annotation.JsonProperty("action")
    private String action;

    @com.fasterxml.jackson.annotation.JsonProperty("timestamp")
    private String timestamp;
}
