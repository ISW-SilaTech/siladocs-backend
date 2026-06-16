package com.siladocs.application.controller;

import com.siladocs.application.dto.GenerateCodeRequest;
import com.siladocs.application.service.AccessCodeService;
import com.siladocs.domain.entity.AccessCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/access-codes")
public class AccessCodeController {

    private final AccessCodeService accessCodeService;

    public AccessCodeController(AccessCodeService accessCodeService) {
        this.accessCodeService = accessCodeService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AccessCode> generateCode(@Valid @RequestBody GenerateCodeRequest request) {
        AccessCode newCode = accessCodeService.generateCode(request);
        return new ResponseEntity<>(newCode, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccessCode>> listCodes() {
        return ResponseEntity.ok(accessCodeService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessCode> getCode(@PathVariable UUID id) {
        return ResponseEntity.ok(accessCodeService.findById(id));
    }
}
