package com.siladocs.application.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AzureBlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);

    @Autowired
    private BlobContainerClient blobContainerClient;

    @Value("${azure.blob-storage.sas-expiry-hours:24}")
    private int sasExpiryHours;

    @Value("${azure.blob-storage.account-name}")
    private String accountName;

    @Value("${azure.blob-storage.account-key}")
    private String accountKey;

    public String uploadSyllabus(MultipartFile file, Long syllabusId) throws Exception {
        return uploadFile(file, "syllabi/" + syllabusId);
    }

    public String uploadTempFile(MultipartFile file) throws Exception {
        String tempFileName = "temp/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        return uploadFile(file, tempFileName);
    }

    public String uploadFile(MultipartFile file, String blobName) throws Exception {
        try {
            log.info("Uploading file to Azure Blob Storage: {}", blobName);

            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

            try (InputStream inputStream = file.getInputStream()) {
                blobClient.upload(inputStream, file.getSize(), true);
            }

            log.info("File uploaded successfully: {}", blobName);
            return blobName;
        } catch (BlobStorageException e) {
            log.error("Error uploading file to Azure Blob Storage: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to Azure Blob Storage", e);
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage());
            throw new RuntimeException("Unexpected error during file upload", e);
        }
    }

        public String generateDownloadSasUrl(String blobName) throws Exception {
        return generateDownloadSasUrl(blobName, sasExpiryHours);
    }

    public String generateDownloadSasUrl(String blobName, int expiryHours) throws Exception {
        try {
            log.info("Generating SAS URL for blob: {} (expires in {}h)", blobName, expiryHours);

            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

            BlobSasPermission permission = new BlobSasPermission()
                    .setReadPermission(true);

            BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(
                    OffsetDateTime.now().plusHours(expiryHours),
                    permission
            );

            String sas = blobClient.generateSas(sasSignatureValues);
            String sasUrl = blobClient.getBlobUrl() + "?" + sas;

            log.info("SAS URL generated successfully for blob: {}", blobName);
            return sasUrl;
        } catch (Exception e) {
            log.error("Error generating SAS URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate SAS URL", e);
        }
    }

    public byte[] downloadFile(String blobName) throws Exception {
        try {
            log.info("Downloading file from Azure Blob Storage: {}", blobName);

            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            return blobClient.downloadContent().toBytes();
        } catch (BlobStorageException e) {
            log.error("Error downloading file: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from Azure Blob Storage", e);
        }
    }

    public void deleteFile(String blobName) throws Exception {
        try {
            log.info("Deleting file from Azure Blob Storage: {}", blobName);

            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            blobClient.delete();

            log.info("File deleted successfully: {}", blobName);
        } catch (BlobStorageException e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from Azure Blob Storage", e);
        }
    }

    public boolean fileExists(String blobName) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            return blobClient.exists();
        } catch (Exception e) {
            log.warn("Error checking if file exists: {}", e.getMessage());
            return false;
        }
    }

    public String uploadBytes(byte[] fileBytes, String fileName, String blobName, String contentType) throws Exception {
        try {
            log.info("Uploading bytes to Azure Blob Storage: {}", blobName);
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            try (InputStream inputStream = new java.io.ByteArrayInputStream(fileBytes)) {
                com.azure.storage.blob.models.BlobHttpHeaders headers = new com.azure.storage.blob.models.BlobHttpHeaders()
                        .setContentType(contentType != null ? contentType : "application/octet-stream");
                blobClient.upload(inputStream, fileBytes.length, true);
                blobClient.setHttpHeaders(headers);
            }
            log.info("Bytes uploaded successfully: {}", blobName);
            return blobName;
        } catch (Exception e) {
            log.error("Error uploading bytes: {}", e.getMessage());
            throw new RuntimeException("Failed to upload bytes to Azure Blob Storage", e);
        }
    }

    public void deleteDirectory(String directoryPrefix) throws Exception {
        try {
            log.info("Deleting directory from Azure Blob Storage: {}", directoryPrefix);

            blobContainerClient.listBlobs()
                    .stream()
                    .filter(blobItem -> blobItem.getName().startsWith(directoryPrefix))
                    .forEach(blobItem -> {
                        try {
                            blobContainerClient.getBlobClient(blobItem.getName()).delete();
                            log.debug("Deleted blob: {}", blobItem.getName());
                        } catch (Exception e) {
                            log.warn("Failed to delete blob {}: {}", blobItem.getName(), e.getMessage());
                        }
                    });

            log.info("Directory deleted successfully: {}", directoryPrefix);
        } catch (Exception e) {
            log.error("Error deleting directory: {}", e.getMessage());
            throw new RuntimeException("Failed to delete directory from Azure Blob Storage", e);
        }
    }
}
