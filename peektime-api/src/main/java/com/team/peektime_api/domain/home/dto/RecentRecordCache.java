package com.team.peektime_api.domain.home.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;

import java.time.LocalDateTime;

public record RecentRecordCache(
        Long completionId,
        String objectKey,
        LocalDateTime recordedAt
) {
    public static RecentRecordCache from(UserMissionCompletion completion) {
        return new RecentRecordCache(
                completion.getId(),
                completion.getObjectKey(),
                completion.getCreatedAt()
        );
    }
}
