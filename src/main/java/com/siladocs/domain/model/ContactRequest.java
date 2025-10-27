package com.siladocs.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity // <-- Marca como tabla
@Table(name = "contact_requests")
public class ContactRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(length = 1024)
    private String message;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    public ContactRequest(String institutionName, String contactName, String email, String phone, String message) {
        this.institutionName = institutionName;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.message = message;
    }
}