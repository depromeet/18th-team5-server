package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.EnjoyType;

import java.util.List;

public record RecommendedMissionResponse(
        HeaderInfo header,
        List<MissionItem> missions
) {

    public record HeaderInfo(
            String title,
            String subtitle
    ) {
        public static HeaderInfo of(String userTypeLabel, String solarTermName) {
            return new HeaderInfo(
                    userTypeLabel + " " + solarTermName + " 미션",
                    solarTermName + "에 어떤 기록을 남겨볼까요?"
            );
        }
    }

    public record MissionItem(
            Long id,
            String title,
            CategoryType categoryType,
            EnjoyType enjoyType,
            int order
    ) {
        public static MissionItem from(RecommendedMissionPool pool, int order) {
            return new MissionItem(
                    pool.getMission().getId(),
                    pool.getMission().getTitle(),
                    pool.getMission().getCategoryType(),
                    pool.getMission().getEnjoyType(),
                    order
            );
        }
    }
}
