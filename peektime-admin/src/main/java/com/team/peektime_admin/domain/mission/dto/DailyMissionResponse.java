package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class DailyMissionResponse {

    private final Long id;
    private final Long missionId;
    private final String missionTitle;
    private final Long solarTermId;
    private final String solarTermName;
    private final LocalDate missionDate;
    private final Integer displayOrder;
    private final boolean pending;
    private final LocalDateTime createdAt;

    public DailyMissionResponse(DailyMission dailyMission) {
        this.id = dailyMission.getId();
        this.missionId = dailyMission.getMission().getId();
        this.missionTitle = dailyMission.getMission().getTitle();
        this.solarTermId = dailyMission.getSolarTerm().getId();
        this.solarTermName = dailyMission.getSolarTerm().getName();
        this.missionDate = dailyMission.getMissionDate();
        this.displayOrder = dailyMission.getDisplayOrder();
        this.pending = dailyMission.isPending();
        this.createdAt = dailyMission.getCreatedAt();
    }
}
