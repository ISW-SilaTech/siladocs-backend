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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/oauth/google")
public class GoogleDriveOAuthController {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveOAuthController.class);

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:https://siladocs-frontend.vercel.app/oauth/google/callback}")
    private String redirectUri;

    private final AzureBlobStorageService azureBlobStorageService;

    public GoogleDriveOAuthController(AzureBlobStorageService azureBlobStorageService) {
        this.azureBlobStorageService = azureBlobStorageService;
    }

    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthorizationUrl() {
        try {
            if (clientId.isBlank()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Google OAuth not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET."));
            }

            String scope = URLEncoder.encode(
                    "https://www.googleapis.com/auth/drive.readonly",
                    StandardCharsets.UTF_8);

            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                    "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&response_type=code" +
                    "&scope=" + scope +
                    "&access_type=offline" +
                    "&prompt=consent";

            return ResponseEntity.ok(Map.of("authUrl", authUrl));
        } catch (Exception e) {
            log.error("Error generating Google auth URL: {}", e.getMessage());
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
            String formBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code";

            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                    formBody, okhttp3.MediaType.parse("application/x-www-form-urlencoded"));
            Request request = new Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(requestBody)
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                String responseBodyStr = response.body() != null ? response.body().string() : "{}";
                if (!response.isSuccessful()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Token exchange failed", "details", responseBodyStr));
                }
                return ResponseEntity.ok(responseBodyStr);
            }
        } catch (Exception e) {
            log.error("Error exchanging Google token: {}", e.getMessage());
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

            String query = URLEncoder.encode(
                    "(mimeType='application/pdf' or mimeType='application/msword' or " +
                    "mimeType='application/vnd.openxmlformats-officedocument.wordprocessingml.document') " +
                    "and trashed=false",
                    StandardCharsets.UTF_8);

            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files" +
                         "?q=" + query +
                         "&fields=files(id,name,mimeType,size,createdTime,modifiedTime,webViewLink)" +
                         "&pageSize=50")
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                String responseBodyStr = response.body() != null ? response.body().string() : "{}";
                if (!response.isSuccessful()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Failed to list files", "details", responseBodyStr));
                }
                return ResponseEntity.ok(responseBodyStr);
            }
        } catch (Exception e) {
            log.error("Error listing Google Drive files: {}", e.getMessage());
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
                    .url("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media")
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            byte[] fileBytes;
            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Failed to download file from Google Drive"));
                }
                fileBytes = response.body().bytes();
            }

            String safeFileName = fileName != null ? fileName : fileId + ".pdf";
            String blobName = syllabusIdStr != null
                    ? "syllabi/" + syllabusIdStr
                    : "syllabi/gdrive-" + UUID.randomUUID() + "_" + safeFileName;

            String uploadedBlobName = azureBlobStorageService.uploadBytes(fileBytes, safeFileName, blobName, "application/pdf");
            String downloadUrl = azureBlobStorageService.generateDownloadSasUrl(uploadedBlobName);

            log.info("Google Drive file '{}' imported to Azure Blob Storage: {}", fileName, uploadedBlobName);
            return ResponseEntity.ok(Map.of(
                    "blobName", uploadedBlobName,
                    "downloadUrl", downloadUrl,
                    "message", "File imported from Google Drive successfully"
            ));
        } catch (Exception e) {
            log.error("Error importing Google Drive file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error importing file: " + e.getMessage()));
        }
    }
}
