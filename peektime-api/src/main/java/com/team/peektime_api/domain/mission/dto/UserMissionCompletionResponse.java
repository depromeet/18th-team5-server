package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;

public record UserMissionCompletionResponse(
        Long completionId
) {
    public static UserMissionCompletionResponse from(UserMissionCompletion completion) {
        return new UserMissionCompletionResponse(completion.getId());
    }
}
