package com.siladocs.application.controller;

import com.siladocs.application.dto.ContactMessageListDto;
import com.siladocs.application.dto.ContactMessageRequest;
import com.siladocs.application.dto.ContactMessageResponse;
import com.siladocs.application.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/contact")
@Tag(name = "Contact", description = "Endpoints para mensajes de contacto")
@RequiredArgsConstructor
@Slf4j
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/send")
    @Operation(summary = "Enviar mensaje de contacto", description = "Permite a un usuario enviar un mensaje de contacto")
    public ResponseEntity<Map<String, Object>> sendContactMessage(
        @Valid @RequestBody ContactMessageRequest request,
        HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            ContactMessageResponse response = contactMessageService.sendMessage(request, ipAddress, userAgent);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Mensaje enviado exitosamente");
            responseBody.put("data", response);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
        } catch (RuntimeException e) {
            log.error("Error sending contact message: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error sending contact message: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error procesando el mensaje de contacto");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/messages")
    @Operation(summary = "Obtener todos los mensajes", description = "Obtiene lista de todos los mensajes de contacto (requiere autenticación de admin)")
    public ResponseEntity<Map<String, Object>> getAllMessages() {
        try {
            List<ContactMessageListDto> messages = contactMessageService.getAllMessages();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", messages);
            response.put("count", messages.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching messages: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error obteniendo los mensajes");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener mensaje por ID", description = "Obtiene los detalles de un mensaje específico (requiere autenticación de admin)")
    public ResponseEntity<Map<String, Object>> getMessageById(@PathVariable UUID id) {
        try {
            Optional<ContactMessageResponse> message = contactMessageService.getMessageById(id);

            if (message.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", message.get());

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("success", false);
                notFoundResponse.put("message", "Mensaje no encontrado");

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
            }
        } catch (Exception e) {
            log.error("Error fetching message {}: {}", id, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error obteniendo el mensaje");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar estado de mensaje", description = "Actualiza el estado de un mensaje (requiere autenticación de admin)")
    public ResponseEntity<Map<String, Object>> updateMessageStatus(
        @PathVariable UUID id,
        @RequestBody Map<String, String> request) {

        try {
            String status = request.get("status");

            if (status == null || status.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "El estado es requerido");

                return ResponseEntity.badRequest().body(errorResponse);
            }

            contactMessageService.updateMessageStatus(id, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estado actualizado exitosamente");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating message status: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error actualizando el estado del mensaje");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar mensaje", description = "Elimina un mensaje de contacto (requiere autenticación de admin)")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable UUID id) {
        try {
            contactMessageService.deleteMessage(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mensaje eliminado exitosamente");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error eliminando el mensaje");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }
}
