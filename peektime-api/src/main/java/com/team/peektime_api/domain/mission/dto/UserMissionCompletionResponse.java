package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.global.common.enums.MissionType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserMissionCompletionResponse {

    private final Long id;
    private final Long userId;
    private final Long missionId;
    private final MissionType missionType;
    private final String imageUrl;
    private final String memo;
    private final LocalDateTime completedAt;

    public UserMissionCompletionResponse(UserMissionCompletion completion) {
        this.id = completion.getId();
        this.userId = completion.getUser().getId();
        this.missionId = completion.getMissionId();
        this.missionType = completion.getMissionType();
        this.imageUrl = completion.getImageUrl();
        this.memo = completion.getMemo();
        this.completedAt = completion.getCompletedAt();
    }
}
