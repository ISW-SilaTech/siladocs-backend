package com.siladocs.infrastructure.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    @Value("${siladocs.storage.bucket}")
    private String bucket;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String upload(MultipartFile file, String objectName) throws Exception {
        String name = objectName == null ? UUID.randomUUID() + "_" + file.getOriginalFilename() : objectName;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(name)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        // URL pública / local (ajusta según entorno)
        return String.format("%s/%s/%s", "http://localhost:9000", bucket, name);
    }
}
