package com.team.peektime_api.global.infra.S3;

import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.S3.dto.PresignedUrlResponse;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
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

    @Value("${cloud.aws.cloudfront.base-url}")
    private String cloudfrontBaseUrl;

    private static final Duration UPLOAD_URL_EXPIRY = Duration.ofMinutes(10);

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

    /**
     * 조회용 이미지 URL. CloudFront(Public) 정적 URL을 조립해 반환한다.
     * presigned 방식의 문제(STS 임시 자격증명 만료로 URL 캐싱 불가 + 요청마다 서명 비용)를 제거.
     */
    public String generatePresignedViewUrl(String objectKey) {
        return cloudfrontBaseUrl + "/" + objectKey;
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
