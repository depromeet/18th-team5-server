package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.global.common.enums.MissionType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record UserMissionCompletionResponse(
        Long completionId,
        Long missionId,
        MissionType missionType,
        OffsetDateTime completedAt
) {
    public static UserMissionCompletionResponse from(UserMissionCompletion completion) {
        return new UserMissionCompletionResponse(
                completion.getId(),
                completion.getMissionId(),
                completion.getMissionType(),
                completion.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }
}
