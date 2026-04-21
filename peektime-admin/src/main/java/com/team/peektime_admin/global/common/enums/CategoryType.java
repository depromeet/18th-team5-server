package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryType {
    FOOD("음식"),
    NATURE("자연"),
    RECORD("기록"),
    PLACE("장소"),
    SENSE("감각");

    private final String description;
}
