package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.Mission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "미션 기록하기 페이지 응답")
@Getter
@Builder
public class MissionRecordPageResponse {

    @Schema(description = "미션 ID", example = "1")
    private Long id;

    @Schema(description = "미션 제목", example = "나만의 여름 음료 개발")
    private String title;

    @Schema(description = "미션 설명", example = "입하 제철 토마토로 상큼한 여름 음료를 만들어보세요")
    private String description;

    public static MissionRecordPageResponse from(Mission mission) {
        return MissionRecordPageResponse.builder()
                .id(mission.getId())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .build();
    }
}