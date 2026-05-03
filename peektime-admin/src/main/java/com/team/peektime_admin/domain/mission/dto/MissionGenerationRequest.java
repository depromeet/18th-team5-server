package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MissionGenerationRequest {

    private int count;
    private String theme;
    private Long solarTermId;
    private UserType userType;
    private EnjoyType enjoyType;
}