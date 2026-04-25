package com.team.peektime_api.domain.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UserRecordRequest {

    @NotNull
    private Long solarTermId;

    @NotNull
    private LocalDate recordDate;

    @NotBlank
    private String imageUrl;

    private String memo;
}
