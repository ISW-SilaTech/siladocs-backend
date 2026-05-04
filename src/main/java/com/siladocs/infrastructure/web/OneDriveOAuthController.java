package com.siladocs.infrastructure.web;

import com.siladocs.application.service.AzureBlobStorageService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/oauth/onedrive")
public class OneDriveOAuthController {

    private static final Logger log = LoggerFactory.getLogger(OneDriveOAuthController.class);

    @Value("${microsoft.oauth.client-id:}")
    private String clientId;

    @Value("${microsoft.oauth.client-secret:}")
    private String clientSecret;

    @Value("${microsoft.oauth.tenant-id:common}")
    private String tenantId;

    @Value("${microsoft.oauth.redirect-uri:http://localhost:3000/oauth/onedrive/callback}")
    private String redirectUri;

    private final AzureBlobStorageService azureBlobStorageService;

    public OneDriveOAuthController(AzureBlobStorageService azureBlobStorageService) {
        this.azureBlobStorageService = azureBlobStorageService;
    }

    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthorizationUrl() {
        try {
            if (clientId.isBlank()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "OneDrive OAuth not configured. Set MICROSOFT_CLIENT_ID and MICROSOFT_CLIENT_SECRET."));
            }

            String scopes = URLEncoder.encode("Files.Read Files.Read.All offline_access", StandardCharsets.UTF_8);
            String authUrl = String.format(
                    "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize" +
                    "?client_id=%s&response_type=code&redirect_uri=%s&scope=%s&response_mode=query",
                    tenantId, clientId,
                    URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                    scopes);

            return ResponseEntity.ok(Map.of("authUrl", authUrl));
        } catch (Exception e) {
            log.error("Error generating OneDrive auth URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error generating authorization URL: " + e.getMessage()));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> exchangeToken(@RequestBody Map<String, String> body) {
        try {
            String code = body.get("code");
            if (code == null || code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }

            OkHttpClient httpClient = new OkHttpClient();
            String tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
            String formBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code" +
                    "&scope=" + URLEncoder.encode("Files.Read Files.Read.All offline_access", StandardCharsets.UTF_8);

            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                    formBody, okhttp3.MediaType.parse("application/x-www-form-urlencoded"));
            Request request = new Request.Builder().url(tokenUrl).post(requestBody).build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "{}";
                if (!response.isSuccessful()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Token exchange failed", "details", responseBody));
                }
                return ResponseEntity.ok(responseBody);
            }
        } catch (Exception e) {
            log.error("Error exchanging OneDrive token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error exchanging token: " + e.getMessage()));
        }
    }

    @PostMapping("/files")
    public ResponseEntity<?> listFiles(@RequestBody Map<String, Object> body) {
        try {
            String accessToken = (String) body.get("accessToken");
            if (accessToken == null || accessToken.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access token is required"));
            }

            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://graph.microsoft.com/v1.0/me/drive/root/children" +
                         "?$filter=endswith(name, '.pdf') or endswith(name, '.docx') or endswith(name, '.doc')" +
                         "&$select=id,name,file,size,createdDateTime,lastModifiedDateTime,webUrl")
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "{}";
                if (!response.isSuccessful()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Failed to list files", "details", responseBody));
                }
                return ResponseEntity.ok(responseBody);
            }
        } catch (Exception e) {
            log.error("Error listing OneDrive files: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error listing files: " + e.getMessage()));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importFile(@RequestBody Map<String, String> body) {
        try {
            String accessToken = body.get("accessToken");
            String fileId = body.get("fileId");
            String fileName = body.get("fileName");
            String syllabusIdStr = body.get("syllabusId");

            if (accessToken == null || fileId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "accessToken and fileId are required"));
            }

            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://graph.microsoft.com/v1.0/me/drive/items/" + fileId + "/content")
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            byte[] fileBytes;
            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Failed to download file from OneDrive"));
                }
                fileBytes = response.body().bytes();
            }

            String safeFileName = fileName != null ? fileName : fileId + ".pdf";
            String blobName = syllabusIdStr != null
                    ? "syllabi/" + syllabusIdStr
                    : "syllabi/onedrive-" + UUID.randomUUID() + "_" + safeFileName;

            String uploadedBlobName = azureBlobStorageService.uploadBytes(fileBytes, safeFileName, blobName, "application/pdf");
            String downloadUrl = azureBlobStorageService.generateDownloadSasUrl(uploadedBlobName);

            log.info("OneDrive file '{}' imported to Azure Blob Storage: {}", fileName, uploadedBlobName);
            return ResponseEntity.ok(Map.of(
                    "blobName", uploadedBlobName,
                    "downloadUrl", downloadUrl,
                    "message", "File imported from OneDrive successfully"
            ));
        } catch (Exception e) {
            log.error("Error importing OneDrive file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error importing file: " + e.getMessage()));
        }
    }
}
