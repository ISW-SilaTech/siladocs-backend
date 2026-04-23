package com.siladocs.application.controller;

import com.siladocs.application.dto.GenerateCodeRequest;
import com.siladocs.application.service.AccessCodeService;
import com.siladocs.domain.entity.AccessCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/access-codes")
public class AccessCodeController {

    private final AccessCodeService accessCodeService;

    // Inyección de dependencias por constructor (Buena práctica)
    public AccessCodeController(AccessCodeService accessCodeService) {
        this.accessCodeService = accessCodeService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AccessCode> generateCode(@Valid @RequestBody GenerateCodeRequest request) {
        AccessCode newCode = accessCodeService.generateCode(request);
        
        // Devolvemos un 201 CREATED junto con los datos del código generado
        return new ResponseEntity<>(newCode, HttpStatus.CREATED);
    }
}
