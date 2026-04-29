package com.siladocs.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);

    @Value("${recaptcha.secret-key}")
    private String recaptchaSecretKey;

    @Value("${recaptcha.score-threshold:0.5}")
    private double scoreThreshold;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public RecaptchaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("reCAPTCHA token is empty");
            return false;
        }

        try {
            String verificationUrl = RECAPTCHA_VERIFY_URL + "?secret=" + recaptchaSecretKey + "&response=" + token;
            String response = restTemplate.postForObject(verificationUrl, null, String.class);

            if (response != null) {
                JsonNode node = objectMapper.readTree(response);

                boolean success = node.get("success").asBoolean();
                double score = node.has("score") ? node.get("score").asDouble() : 1.0;

                if (success && score >= scoreThreshold) {
                    log.info("reCAPTCHA validation successful with score: {}", score);
                    return true;
                } else {
                    log.warn("reCAPTCHA validation failed. Success: {}, Score: {}", success, score);
                    return false;
                }
            }
        } catch (RestClientException e) {
            log.error("Error calling reCAPTCHA verification service: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error validating reCAPTCHA token: {}", e.getMessage(), e);
        }

        return false;
    }
}
