package com.siladocs.infrastructure.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AzureBlobStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageConfig.class);

    @Value("${azure.blob-storage.connection-string}")
    private String connectionString;

    @Value("${azure.blob-storage.container-name}")
    private String containerName;

    @Bean
    public BlobServiceClient blobServiceClient() {
        log.info("Initializing Azure Blob Storage client");
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    @Bean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
        log.info("Getting blob container client for container: {}", containerName);
        return blobServiceClient.getBlobContainerClient(containerName);
    }
}
