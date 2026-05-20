package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.global.common.enums.MissionType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record UserMissionCompletionDetailResponse(
        Long completionId,
        Long missionId,
        MissionType missionType,
        String objectKey,
        String presignedImageUrl,
        String memo,
        OffsetDateTime completedAt
) {
    public static UserMissionCompletionDetailResponse of(UserMissionCompletion completion, String presignedImageUrl) {
        return new UserMissionCompletionDetailResponse(
                completion.getId(),
                completion.getMission().getId(),
                completion.getMissionType(),
                completion.getObjectKey(),
                presignedImageUrl,
                completion.getMemo(),
                completion.getCompletedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }
}
