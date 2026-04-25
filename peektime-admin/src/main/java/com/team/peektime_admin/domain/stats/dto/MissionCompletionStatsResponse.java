package com.team.peektime_admin.domain.stats.dto;

import com.team.peektime_admin.domain.stats.entity.MissionCompletionStats;
import com.team.peektime_admin.global.common.enums.MissionType;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MissionCompletionStatsResponse {

    private final Long id;
    private final Long missionId;
    private final String missionTitle;
    private final MissionType missionType;
    private final Long solarTermId;
    private final LocalDate referenceDate;
    private final Integer completionCount;

    public MissionCompletionStatsResponse(MissionCompletionStats stats) {
        this.id = stats.getId();
        this.missionId = stats.getMission().getId();
        this.missionTitle = stats.getMission().getTitle();
        this.missionType = stats.getMissionType();
        this.solarTermId = stats.getSolarTerm().getId();
        this.referenceDate = stats.getReferenceDate();
        this.completionCount = stats.getCompletionCount();
    }
}
