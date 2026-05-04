package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.global.common.enums.MissionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record UserMissionCompletionRequest(

        @NotNull
        MissionType missionType,

        @Size(max = 500)
        String objectKey,

        @Size(max = 200)
        String memo,

        OffsetDateTime completedAt
) {}
