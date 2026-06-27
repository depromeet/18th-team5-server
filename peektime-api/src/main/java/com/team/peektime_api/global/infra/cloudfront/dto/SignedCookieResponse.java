package com.team.peektime_api.global.infra.cloudfront.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Schema(description = "CloudFront Signed Cookie 응답")
@Getter
@Builder
public class SignedCookieResponse {

    @Schema(description = "CloudFront-Policy 쿠키 값")
    private String policy;

    @Schema(description = "CloudFront-Signature 쿠키 값")
    private String signature;

    @Schema(description = "CloudFront-Key-Pair-Id 쿠키 값")
    private String keyPairId;

    @Schema(description = "쿠키 만료 시각 (Unix timestamp)")
    private long expiresAt;

    public static SignedCookieResponse of(
            String policy,
            String signature,
            String keyPairId,
            Instant expiresAt
    ) {
        return SignedCookieResponse.builder()
                .policy(policy)
                .signature(signature)
                .keyPairId(keyPairId)
                .expiresAt(expiresAt.getEpochSecond())
                .build();
    }
}
