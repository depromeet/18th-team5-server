package com.team.peektime_api.domain.stats.dto;

import com.team.peektime_api.global.common.enums.MissionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class MissionCompletionStatsResponse {

    private Long id;
    private Long missionId;
    private String missionTitle;
    private MissionType missionType;
    private Long solarTermId;
    private LocalDate referenceDate;
    private Integer completionCount;
}
