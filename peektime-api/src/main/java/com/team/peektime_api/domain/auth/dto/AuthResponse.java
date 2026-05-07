package com.team.peektime_api.domain.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
    public static AuthResponse of(String accessToken, String refreshToken, boolean isNewUser) {
        return new AuthResponse(accessToken, refreshToken, isNewUser);
    }
}
