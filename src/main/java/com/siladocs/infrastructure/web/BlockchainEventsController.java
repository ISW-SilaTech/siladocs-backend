package com.siladocs.infrastructure.web;

import com.siladocs.application.service.BlockchainEventEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/blockchain/events")
public class BlockchainEventsController {

    private static final Logger log = LoggerFactory.getLogger(BlockchainEventsController.class);
    private final BlockchainEventEmitterService emitterService;

    public BlockchainEventsController(BlockchainEventEmitterService emitterService) {
        this.emitterService = emitterService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String sessionId) {
        log.info("SSE subscription: {}", sessionId);
        return emitterService.register(sessionId);
    }
}
