package com.siladocs.siladocs_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.siladocs") // Escanea TODO bajo com.siladocs
@EnableJpaRepositories(basePackages = { // Busca JpaRepositories aquí
        "com.siladocs.domain.repository", // Para ContactRequestRepository
        "com.siladocs.infrastructure.persistence.jparepository" // Para UserJpaRepository, etc.
})
@EntityScan(basePackages = { // Busca @Entity aquí
        "com.siladocs.domain.model", // Para ContactRequest
        "com.siladocs.infrastructure.persistence.entity" // Para UserEntity, etc.
})
public class SiladocsBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiladocsBackendApplication.class, args);
    }
}