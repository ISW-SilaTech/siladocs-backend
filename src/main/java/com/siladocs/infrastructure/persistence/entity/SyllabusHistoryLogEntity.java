package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/**
 * Esta entidad representa un "Bloque" en la cadena de historial de un sílabo.
 * Corresponde a la tabla 'syllabus_history_log'.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "syllabus_history_log")
public class SyllabusHistoryLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el sílabo principal
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private SyllabusEntity syllabus;

    // ID del usuario que realizó la acción
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String action; // Ej: "CREADO", "MODIFICADO", "APROBADO"

    @Column(name = "change_timestamp", nullable = false)
    private Instant changeTimestamp;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash; // Hash SHA-256 del archivo en este punto

    // 🔹 Campos manejados por el Trigger de PostgreSQL 🔹
    // Le decimos a JPA/Hibernate que no intente insertar o actualizar estos campos,
    // ya que el trigger 'fn_calculate_chain_hash()' en la BD lo hará.

    @Column(name = "previous_chain_hash", nullable = false, length = 64, insertable = false, updatable = false)
    private String previousChainHash; // Hash del bloque anterior

    @Column(name = "chain_hash", nullable = false, length = 64, insertable = false, updatable = false)
    private String chainHash; // Hash de este bloque

    /**
     * Constructor utilizado por SyllabusService para crear un nuevo registro.
     * Los campos 'previousChainHash' y 'chainHash' son omitidos
     * porque son calculados y asignados por el trigger de la base de datos.
     */
    public SyllabusHistoryLogEntity(SyllabusEntity syllabus, Long userId, String action, Instant changeTimestamp, String fileHash) {
        this.syllabus = syllabus;
        this.userId = userId;
        this.action = action;
        this.changeTimestamp = changeTimestamp;
        this.fileHash = fileHash;
    }
}