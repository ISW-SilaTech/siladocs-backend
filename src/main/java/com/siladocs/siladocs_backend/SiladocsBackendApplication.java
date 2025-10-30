package com.siladocs.siladocs_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.siladocs")
// ⬇️ CORRECCIÓN AQUÍ: Añade el paquete 'domain.repository' al array
@EnableJpaRepositories(basePackages = {
        "com.siladocs.infrastructure.persistence.jparepository", // Para UserJpaRepository, etc.
        "com.siladocs.domain.repository" // 🔹 Para ContactRequestRepository
})
@EntityScan(basePackages = { // Busca @Entity aquí
        "com.siladocs.domain.model",
        "com.siladocs.infrastructure.persistence.entity"
})
public class SiladocsBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiladocsBackendApplication.class, args);
    }
}