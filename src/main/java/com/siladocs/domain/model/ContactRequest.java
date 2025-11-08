package com.siladocs.domain.model;

// 🔹 Imports de JPA eliminados
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
// ⬇️ Anotaciones JPA eliminadas
public class ContactRequest {

    private Long id;
    private String institutionName;
    private String contactName;
    private String email;
    private String phone;
    private String message;
    private String status = "PENDING";
    private Instant createdAt = Instant.now();

    public ContactRequest(String institutionName, String contactName, String email, String phone, String message) {
        this.institutionName = institutionName;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.message = message;
    }
}