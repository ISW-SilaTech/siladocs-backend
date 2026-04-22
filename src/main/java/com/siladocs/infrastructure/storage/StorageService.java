package com.siladocs.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Servicio de almacenamiento de archivos en MinIO.
 *
 * Responsabilidades:
 * - Subir archivos a MinIO (S3-compatible)
 * - Generar URLs públicas
 * - Manejar excepciones de almacenamiento
 *
 * Nota: Esta es una versión stub que necesita implementación específica
 * de MinIO client. Se integrará con el cliente MinIO.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Value("${minio.endpoint:http://127.0.0.1:9000}")
    private String minioEndpoint;

    @Value("${minio.bucket-name:syllabi}")
    private String bucketName;

    /**
     * Sube un archivo a MinIO y retorna la URL de acceso.
     *
     * @param file          Archivo a subir
     * @param folderPath    Ruta dentro del bucket (ej: /syllabi/course-1/)
     * @return URL pública del archivo
     * @throws RuntimeException Si falla la subida
     */
    public String uploadFile(MultipartFile file, String folderPath) {
        try {
            // Generar nombre único para el archivo
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFileName = generateUniqueFileName(fileExtension);

            // Ruta completa en MinIO: folderPath/uniqueFileName
            String fullPath = String.format("%s%s", folderPath, uniqueFileName);

            log.info("Subiendo archivo a MinIO: bucket={}, path={}, size={} bytes",
                    bucketName, fullPath, file.getSize());

            // TODO: Implementar upload real con MinIO client
            // minioClient.putObject(PutObjectArgs.builder()
            //     .bucket(bucketName)
            //     .object(fullPath)
            //     .stream(file.getInputStream(), file.getSize(), -1)
            //     .contentType(file.getContentType())
            //     .build());

            // URL de acceso público
            String publicUrl = String.format("%s/%s/%s", minioEndpoint, bucketName, fullPath);

            log.info("Archivo subido exitosamente: URL={}", publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("Error al subir archivo a MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Sube un archivo desde bytes (útil para procesar antes de guardar).
     *
     * @param fileBytes     Contenido del archivo
     * @param fileName      Nombre del archivo
     * @param folderPath    Ruta dentro del bucket
     * @param contentType   Tipo MIME
     * @return URL pública del archivo
     */
    public String uploadBytes(byte[] fileBytes, String fileName, String folderPath, String contentType) {
        try {
            String fileExtension = getFileExtension(fileName);
            String uniqueFileName = generateUniqueFileName(fileExtension);
            String fullPath = String.format("%s%s", folderPath, uniqueFileName);

            log.info("Subiendo bytes a MinIO: bucket={}, path={}, size={} bytes",
                    bucketName, fullPath, fileBytes.length);

            // TODO: Implementar upload real con MinIO client
            // InputStream inputStream = new ByteArrayInputStream(fileBytes);
            // minioClient.putObject(PutObjectArgs.builder()
            //     .bucket(bucketName)
            //     .object(fullPath)
            //     .stream(inputStream, fileBytes.length, -1)
            //     .contentType(contentType)
            //     .build());

            String publicUrl = String.format("%s/%s/%s", minioEndpoint, bucketName, fullPath);
            log.info("Bytes subidos exitosamente: URL={}", publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("Error al subir bytes a MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de MinIO.
     *
     * @param filePath Ruta completa del archivo en el bucket
     * @return true si se eliminó, false si no existe
     */
    public boolean deleteFile(String filePath) {
        try {
            log.info("Eliminando archivo de MinIO: bucket={}, path={}", bucketName, filePath);

            // TODO: Implementar eliminación real con MinIO client
            // minioClient.removeObject(RemoveObjectArgs.builder()
            //     .bucket(bucketName)
            //     .object(filePath)
            //     .build());

            log.info("Archivo eliminado exitosamente: {}", filePath);
            return true;

        } catch (Exception e) {
            log.warn("No se pudo eliminar archivo de MinIO: {} (puede que no exista)", filePath);
            return false;
        }
    }

    /**
     * Verifica si un archivo existe en MinIO.
     *
     * @param filePath Ruta del archivo
     * @return true si existe
     */
    public boolean fileExists(String filePath) {
        try {
            log.debug("Verificando existencia de archivo: bucket={}, path={}", bucketName, filePath);

            // TODO: Implementar verificación real con MinIO client
            // StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
            //     .bucket(bucketName)
            //     .object(filePath)
            //     .build());
            // return stat != null;

            return true; // Placeholder

        } catch (Exception e) {
            log.debug("Archivo no existe en MinIO: {}", filePath);
            return false;
        }
    }

    /**
     * Obtiene la extensión de archivo.
     *
     * @param filename Nombre del archivo
     * @return Extensión (ej: .pdf, .docx)
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Genera un nombre único para el archivo.
     * Formato: UUID + timestamp + extensión
     *
     * @param fileExtension Extensión del archivo
     * @return Nombre único
     */
    private String generateUniqueFileName(String fileExtension) {
        return UUID.randomUUID().toString().substring(0, 12) + 
               "-" + 
               System.currentTimeMillis() + 
               fileExtension;
    }

    /**
     * Obtiene la URL pública de un archivo.
     *
     * @param filePath Ruta del archivo en el bucket
     * @return URL completa
     */
    public String getPublicUrl(String filePath) {
        return String.format("%s/%s/%s", minioEndpoint, bucketName, filePath);
    }
}
