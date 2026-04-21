package com.team.peektime_admin.domain.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MissionGenerationRequest {

    private int count;
    private String theme;
}