package com.team.peektime_api.global.infra.S3;

import com.team.peektime_api.global.infra.S3.dto.PresignedUrlResponse;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "S3", description = "S3 파일 관리")
@Validated
@RestController
@RequestMapping("/api/v1/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @Operation(summary = "Presigned URL 발급", description = "S3 직접 업로드용 Presigned URL을 발급합니다.(유효시간 10분)")
    @GetMapping("/presigned-url")
    public SuccessResponse<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam @Pattern(regexp = "image/jpeg|image/png|image/heic") String contentType
    ) {
        PresignedUrlResponse response = s3Service.generatePresignedUrl(fileName, contentType);
        return SuccessResponse.of(SuccessCode.S3_PRESIGNED_URL, response);
    }

    @Operation(summary = "S3 이미지 삭제", description = "S3에 업로드된 이미지를 삭제합니다.")
    @DeleteMapping("/image")
    public SuccessResponse<Void> deleteImage(
            @RequestParam String objectKey
    ) {
        s3Service.deleteImage(objectKey);
        return SuccessResponse.of(SuccessCode.S3_IMAGE_DELETED, null);
    }
}
