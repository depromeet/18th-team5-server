package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IntensityType {
    LIGHT("가벼운"),
    ACTIVE("적극적");

    private final String description;
}
