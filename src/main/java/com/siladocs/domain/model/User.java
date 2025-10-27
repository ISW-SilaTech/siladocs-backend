package com.siladocs.domain.model;

// ⬇️ REMOVE JPA imports: jakarta.persistence.*
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// ⬇️ REMOVE @Entity
// ⬇️ REMOVE @Table(name = "users")
public class User {

    // ⬇️ REMOVE @Id
    // ⬇️ REMOVE @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // ⬇️ REMOVE @Column(...)
    private String name;

    // ⬇️ REMOVE @Column(...)
    private String email;

    // ⬇️ REMOVE @Column(...)
    private String passwordHash;

    // ⬇️ REMOVE @Column(...)
    private String role;

    // ⬇️ REMOVE @Column(...)
    private Long institutionId;

    // ⬇️ REMOVE @Column(...)
    private Instant createdAt = Instant.now();

    // Constructor sin id para creación más limpia
    public User(String name, String email, String passwordHash, String role, Long institutionId) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.institutionId = institutionId;
        // Asignar createdAt aquí también si quieres que se establezca al crear
        this.createdAt = Instant.now();
    }
}