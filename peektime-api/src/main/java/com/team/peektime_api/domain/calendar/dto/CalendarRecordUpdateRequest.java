package com.team.peektime_api.domain.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CalendarRecordUpdateRequest(

        @NotBlank(message = "사진은 필수입니다")
        @Size(max = 500)
        String objectKey,

        @Size(max = 200)
        String memo
) {}
