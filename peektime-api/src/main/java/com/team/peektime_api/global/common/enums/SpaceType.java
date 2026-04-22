package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpaceType {
    INDOOR("실내", "집이나 실내 공간에서 수행"),
    OUTDOOR("실외", "밖에서 수행");

    private final String label;
    private final String description;
}
