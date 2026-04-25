package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.global.common.enums.MissionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UserMissionCompletionRequest {

    @NotNull
    private Long missionId;

    @NotNull
    private MissionType missionType;

    private String imageUrl;

    private String memo;
}
