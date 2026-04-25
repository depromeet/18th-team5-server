package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecommendedMissionPoolResponse {

    private final Long id;
    private final Long missionId;
    private final String missionTitle;
    private final Long solarTermId;
    private final String solarTermName;
    private final UserType userType;
    private final Integer displayOrder;
    private final LocalDateTime createdAt;

    public RecommendedMissionPoolResponse(RecommendedMissionPool pool) {
        this.id = pool.getId();
        this.missionId = pool.getMission().getId();
        this.missionTitle = pool.getMission().getTitle();
        this.solarTermId = pool.getSolarTerm().getId();
        this.solarTermName = pool.getSolarTerm().getName();
        this.userType = pool.getUserType();
        this.displayOrder = pool.getDisplayOrder();
        this.createdAt = pool.getCreatedAt();
    }
}
