package com.siladocs.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlockchainEventEmitterService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainEventEmitterService.class);
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter register(String sessionId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));
        log.info("SSE registered: {}", sessionId);
        return emitter;
    }

    public void emit(String sessionId, String eventType, String message, String detail, int progress) {
        if (sessionId == null || sessionId.isBlank()) return;
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("eventType", eventType);
            data.put("message", message);
            data.put("detail", detail != null ? detail : "");
            data.put("timestamp", Instant.now().toEpochMilli());
            data.put("progress", progress);

            String json = objectMapper.writeValueAsString(data);

            emitter.send(SseEmitter.event().name(eventType).data(json));
            log.debug("SSE sent: {} -> {} ({}%)", sessionId, eventType, progress);
        } catch (IOException e) {
            log.warn("SSE send failed for {}: {}", sessionId, e.getMessage());
            emitters.remove(sessionId);
        }
    }

    public void complete(String sessionId) {
        if (sessionId == null) return;
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }

    public void emitError(String sessionId, String errorMessage) {
        emit(sessionId, "error", errorMessage, "", 0);
        complete(sessionId);
    }
}
