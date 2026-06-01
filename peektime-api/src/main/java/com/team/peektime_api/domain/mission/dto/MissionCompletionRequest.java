package com.team.peektime_api.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "미션 완료 기록 요청")
public record MissionCompletionRequest(

        @Schema(description = "S3 오브젝트 키", example = "missions/2026/05/29/uuid.jpg")
        @NotBlank(message = "사진은 필수입니다")
        @Size(max = 500)
        String objectKey,

        @Schema(description = "한줄 메모", example = "맛있었다!")
        @Size(max = 200)
        String memo
) {}