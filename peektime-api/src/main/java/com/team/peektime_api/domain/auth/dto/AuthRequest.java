package com.team.peektime_api.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(

        @NotBlank
        @Size(max = 36)
        String deviceUuid
) {}
