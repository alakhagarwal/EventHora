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
    private S3Presigner s3Presigner;

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Value("${aws.region}")
    private String awsRegion;

    public String uploadFile(MultipartFile file, String folder) throws IOException {

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String key = folder + "/" + UUID.randomUUID().toString() + extension;

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes()));

        return "https://" + bucketName + ".s3." + awsRegion + ".amazonaws.com/" + key;
    }

    public void deleteFile(String fileUrlOrKey) {
        String key = extractKeyFromUrl(fileUrlOrKey);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

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

    public byte[] downloadFile(String key) {
        String actualKey = extractKeyFromUrl(key);
        ResponseBytes<GetObjectResponse> objectAsBytes =
                s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(actualKey)
                        .build());
        return objectAsBytes.asByteArray();
    }

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
