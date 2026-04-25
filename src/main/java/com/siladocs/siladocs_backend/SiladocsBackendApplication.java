package com.siladocs.siladocs_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.siladocs")
@EnableJpaRepositories(basePackages = {
        "com.siladocs.infrastructure.persistence.jparepository", 
        "com.siladocs.domain.repository" 
})
@EntityScan(basePackages = { 
        "com.siladocs.domain.model",
        "com.siladocs.infrastructure.persistence.entity",
        "com.siladocs.domain.entity"
})
public class SiladocsBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiladocsBackendApplication.class, args);
    }
}
