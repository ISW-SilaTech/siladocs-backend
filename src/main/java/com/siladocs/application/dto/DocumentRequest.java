package com.siladocs.application.dto;

public record DocumentRequest(
        String fileName,
        String fileType,
        Long fileSize,
        String hash
) {}
