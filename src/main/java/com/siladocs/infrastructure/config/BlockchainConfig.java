package com.siladocs.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuración de Blockchain para integración con Hyperledger Fabric.
 * Proporciona un RestClient configurado para comunicarse con la API REST del middleware Python (FastAPI).
 */
@Configuration
public class BlockchainConfig {

    @Value("${blockchain.fabric.api.url:http://127.0.0.1:8000}")
    private String fabricApiUrl;

    @Value("${blockchain.fabric.api.timeout.connect:10000}")
    private int connectTimeout;

    @Value("${blockchain.fabric.api.timeout.read:30000}")
    private int readTimeout;

    /**
     * Bean RestClient optimizado para comunicación con la API de Fabric.
     *
     * @return RestClient configurado
     */
    @Bean("fabricRestClient")
    public RestClient fabricRestClient() {
        return RestClient.builder()
                .baseUrl(fabricApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Expone la URL de la API de Fabric como bean (útil para logs y debugging).
     *
     * @return URL del API de Fabric
     */
    @Bean("fabricApiUrl")
    public String fabricApiUrl() {
        return fabricApiUrl;
    }
}
