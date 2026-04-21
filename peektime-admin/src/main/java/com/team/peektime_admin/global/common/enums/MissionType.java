package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionType {
    DAILY("오늘의 미션"),
    RECOMMENDED("추천 미션");

    private final String description;
}
