package com.siladocs.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class BlockchainConfig {

    @Value("${blockchain.fabric.api.url:http://127.0.0.1:8000}")
    private String fabricApiUrl;

    @Value("${blockchain.fabric.api.timeout.connect:10000}")
    private int connectTimeout;

    @Value("${blockchain.fabric.api.timeout.read:30000}")
    private int readTimeout;

    @Bean("fabricRestTemplate")
    public RestTemplate fabricRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    @Bean("fabricApiUrl")
    public String fabricApiUrl() {
        return fabricApiUrl;
    }
}
