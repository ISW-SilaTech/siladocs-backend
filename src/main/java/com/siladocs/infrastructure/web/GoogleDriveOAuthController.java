package com.siladocs.infrastructure.web;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.siladocs.application.service.AzureBlobStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.*;

@RestController
@RequestMapping("/oauth/google")
public class GoogleDriveOAuthController {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveOAuthController.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "SilaDocs";

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:http://localhost:3000/oauth/google/callback}")
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

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets clientSecrets = buildClientSecrets();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets,
                    Collections.singletonList(DriveScopes.DRIVE_READONLY))
                    .setAccessType("offline")
                    .build();

            String authUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .build();

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

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets clientSecrets = buildClientSecrets();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets,
                    Collections.singletonList(DriveScopes.DRIVE_READONLY))
                    .build();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            return ResponseEntity.ok(Map.of(
                    "accessToken", tokenResponse.getAccessToken(),
                    "expiresIn", tokenResponse.getExpiresInSeconds()
            ));
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
            int pageSize = body.containsKey("pageSize") ? (int) body.get("pageSize") : 20;

            if (accessToken == null || accessToken.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access token is required"));
            }

            Drive drive = buildDriveService(accessToken);
            String query = "mimeType='application/pdf' or mimeType='application/msword' or " +
                           "mimeType='application/vnd.openxmlformats-officedocument.wordprocessingml.document'";

            FileList result = drive.files().list()
                    .setQ(query + " and trashed=false")
                    .setPageSize(pageSize)
                    .setFields("files(id, name, mimeType, size, createdTime, modifiedTime, webViewLink)")
                    .execute();

            List<Map<String, Object>> files = new ArrayList<>();
            for (File file : result.getFiles()) {
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("id", file.getId());
                fileMap.put("name", file.getName());
                fileMap.put("mimeType", file.getMimeType());
                fileMap.put("size", file.getSize());
                fileMap.put("webViewLink", file.getWebViewLink());
                files.add(fileMap);
            }

            return ResponseEntity.ok(Map.of("files", files));
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

            Drive drive = buildDriveService(accessToken);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            byte[] fileBytes = outputStream.toByteArray();

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

    private GoogleClientSecrets buildClientSecrets() {
        String secretsJson = String.format(
                "{\"web\":{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"redirect_uris\":[\"%s\"]," +
                "\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\"," +
                "\"token_uri\":\"https://oauth2.googleapis.com/token\"}}",
                clientId, clientSecret, redirectUri);
        try {
            return GoogleClientSecrets.load(JSON_FACTORY, new StringReader(secretsJson));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build client secrets", e);
        }
    }

    private Drive buildDriveService(String accessToken) throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));
        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
