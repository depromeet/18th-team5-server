package com.team.peektime_api.domain.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ImageUploadConfirmRequest(

        @NotBlank
        String objectKey,

        @NotNull
        Long userId,

        @NotNull
        Long solarTermId,

        @NotNull
        LocalDate recordDate,

        String memo
) {}
