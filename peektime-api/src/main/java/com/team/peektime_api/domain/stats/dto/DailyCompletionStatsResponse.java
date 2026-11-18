package com.team.peektime_api.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Schema(description = "일별 미션 수행 통계 응답")
@Getter
@Builder
public class DailyCompletionStatsResponse {

    @Schema(description = "통계 기준 날짜", example = "2026-10-01")
    private LocalDate statDate;

    @Schema(description = "미션 완료 수", example = "1523")
    private Long completionCount;

    @Schema(description = "사진 포함 완료 수", example = "412")
    private Long photoCount;

    public static DailyCompletionStatsResponse from(DailyCompletionStatsProjection projection) {
        return DailyCompletionStatsResponse.builder()
                .statDate(projection.getStatDate())
                .completionCount(projection.getCompletionCount())
                .photoCount(projection.getPhotoCount())
                .build();
    }
}