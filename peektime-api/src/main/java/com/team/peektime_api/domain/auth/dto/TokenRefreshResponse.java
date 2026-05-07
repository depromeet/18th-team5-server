package com.team.peektime_api.domain.auth.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
    public static TokenRefreshResponse of(String accessToken, String refreshToken) {
        return new TokenRefreshResponse(accessToken, refreshToken);
    }
}
