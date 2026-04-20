package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryType {
    FOOD("음식", "제철 음식, 요리 관련 미션"),
    NATURE("자연", "자연 감상, 야외 활동 미션"),
    RECORD("기록", "사진, 일기 등 기록 미션"),
    PLACE("장소", "특정 장소 방문 미션"),
    SENSE("감각", "오감을 활용한 미션");

    private final String label;
    private final String description;
}
