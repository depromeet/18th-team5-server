package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnjoyType {
    NATURE_OUTDOOR("자연/야외 활동", "자연/야외", "자연이나 야외에서 즐기는 활동"),
    SEASONAL_FOOD("제철 음식/요리", "제철음식", "제철 음식을 맛보거나 요리하는 활동"),
    CULTURE_CONTENT("감성 콘텐츠/문화", "감성콘텐츠", "감성적인 콘텐츠나 문화 활동");

    private final String label;
    private final String shortLabel;
    private final String description;
}