package com.team.peektime_admin.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "사용자 랭킹 응답")
@Getter
@Builder
public class UserRankingResponse {

    @Schema(description = "순위")
    private int rank;

    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "미션 완료 횟수")
    private Long completionCount;

    public static UserRankingResponse of(int rank, UserRankingProjection projection) {
        return UserRankingResponse.builder()
                .rank(rank)
                .userId(projection.getUserId())
                .completionCount(projection.getCompletionCount())
                .build();
    }
}