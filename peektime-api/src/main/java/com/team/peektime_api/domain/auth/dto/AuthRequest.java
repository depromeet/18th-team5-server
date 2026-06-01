package com.team.peektime_api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "인증 요청")
public record AuthRequest(

        @Schema(description = "디바이스 UUID", example = "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA")
        @NotBlank
        @Size(max = 36)
        String deviceUuid
) {}
