package com.team.peektime_api.global.infra.S3;

import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.S3.dto.PresignedUrlResponse;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
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
    private static final Duration VIEW_URL_EXPIRY = Duration.ofDays(7);

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
        // 응급처치: Redis 캐시에 저장된 만료(ExpiredToken)된 URL이 계속 응답되는 문제로
        // 캐시 조회/저장을 우회하고 매 요청마다 새 presigned URL을 생성한다.
        // (캐시 코드는 추후 CloudFront 전환 시 정리 예정)
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

    public void validateObjectExists(String objectKey) {
        if (objectKey == null) return;
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new BusinessException(ErrorCode.S3_OBJECT_NOT_FOUND);
            }
            throw e;
        }
    }

    public void deleteImage(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }
}
