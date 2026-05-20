package com.team.peektime_api.domain.home.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;

public record RecentRecordCache(
        Long completionId,
        String objectKey
) {
    public static RecentRecordCache from(UserMissionCompletion completion) {
        return new RecentRecordCache(
                completion.getId(),
                completion.getObjectKey()
        );
    }
}