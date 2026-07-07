package com.eventHora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner; // Injected from your S3Config

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Uploads a file to S3 with a unique UUID-based filename.
     * Sets the correct Content-Type so browsers can display it properly.
     *
     * @param file The file from the HTTP request
     * @param folder The folder path in S3 (e.g., "events/banners")
     * @return The public URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        // 1. Generate a unique filename to prevent overwriting
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String key = folder + "/" + UUID.randomUUID().toString() + extension;

        // 2. Upload to S3
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType()) // Crucial for browsers to render images/PDFs inline
                        .build(),
                RequestBody.fromBytes(file.getBytes()));

        // 3. Return the standard public URL so it can be saved in the database (e.g., Event.bannerUrl)
        return "https://" + bucketName + ".s3." + awsRegion + ".amazonaws.com/" + key;
    }

    /**
     * Deletes a file from S3 by its key (or full URL).
     */
    public void deleteFile(String fileUrlOrKey) {
        String key = extractKeyFromUrl(fileUrlOrKey);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    /**
     * Generates a temporary Presigned URL for private files (e.g., Ticket PDFs).
     * The URL will automatically expire after the specified duration.
     */
    public String generatePresignedUrl(String fileUrlOrKey, Duration duration) {
        String key = extractKeyFromUrl(fileUrlOrKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Downloads raw file bytes.
     */
    public byte[] downloadFile(String key) {
        String actualKey = extractKeyFromUrl(key);
        ResponseBytes<GetObjectResponse> objectAsBytes =
                s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(actualKey)
                        .build());
        return objectAsBytes.asByteArray();
    }

    /**
     * Helper to extract the exact S3 object key from a full URL.
     * If the input is already a key (doesn't start with http), it returns it directly.
     */
    private String extractKeyFromUrl(String url) {
        if (url != null && url.startsWith("http")) {
            String prefix = ".amazonaws.com/";
            int index = url.indexOf(prefix);
            if (index != -1) {
                return url.substring(index + prefix.length());
            }
        }
        return url;
    }
}
