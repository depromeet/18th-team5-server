package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {
    NATURE_EXPLORER("제철을 쫓는 탐험가", "밖에서 적극적으로 활동하는 타입"),
    NEIGHBORHOOD_WALKER("일상 속 제철 산책가", "밖에서 가볍게 활동하는 타입"),
    SEASONAL_GOURMET("제철을 채우는 라이프 크리에이터", "실내에서 적극적으로 활동하는 타입"),
    DAILY_OBSERVER("제철을 음미하는 감상가", "실내에서 가볍게 활동하는 타입");

    private final String label;
    private final String description;
}
