package com.team.peektime_admin.domain.mission.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkRecommendedMissionRequest {
    private List<Long> missionIds;
    private Long solarTermId;
    private String userType;
}