package com.team.peektime_api.global.infra.S3;

import com.team.peektime_api.global.infra.S3.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private static final Duration UPLOAD_URL_EXPIRY = Duration.ofMinutes(10);
    private static final Duration VIEW_URL_EXPIRY = Duration.ofDays(1);

    public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType) {
        String objectKey = "images/" + UUID.randomUUID() + "_" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(r -> r
                .signatureDuration(UPLOAD_URL_EXPIRY)
                .putObjectRequest(putObjectRequest)
        );

        return new PresignedUrlResponse(presignedRequest.url().toString(), objectKey);
    }

    public String generatePresignedViewUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
                .signatureDuration(VIEW_URL_EXPIRY)
                .getObjectRequest(getObjectRequest)
        );

        return presignedRequest.url().toString();
    }

    public String getObjectUrl(String objectKey) {
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucket, objectKey);
    }

    public void deleteImage(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }
}
