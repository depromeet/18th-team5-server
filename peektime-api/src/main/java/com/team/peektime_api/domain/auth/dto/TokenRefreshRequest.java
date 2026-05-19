package com.team.peektime_api.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(

        @NotBlank
        String refreshToken
) {}
