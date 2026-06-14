package com.team.peektime_api.domain.calendar.dto;

import jakarta.validation.constraints.Size;

public record CalendarRecordUpdateRequest(

        @Size(max = 500)
        String objectKey,

        @Size(max = 200)
        String memo
) {}
