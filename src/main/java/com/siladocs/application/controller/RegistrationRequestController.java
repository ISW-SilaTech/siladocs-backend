package com.siladocs.application.controller;

import com.siladocs.application.dto.CreateRegistrationRequestDto;
import com.siladocs.application.dto.ReviewRegistrationRequestDto;
import com.siladocs.application.service.RegistrationRequestService;
import com.siladocs.domain.entity.RegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/registration-requests")
public class RegistrationRequestController {

    private final RegistrationRequestService service;

    public RegistrationRequestController(RegistrationRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RegistrationRequest> submit(@Valid @RequestBody CreateRegistrationRequestDto dto) {
        return new ResponseEntity<>(service.submit(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RegistrationRequest>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistrationRequest> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<RegistrationRequest> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewRegistrationRequestDto dto) {
        return ResponseEntity.ok(service.approve(id, dto));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<RegistrationRequest> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewRegistrationRequestDto dto) {
        return ResponseEntity.ok(service.reject(id, dto));
    }
}
