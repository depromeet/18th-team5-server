package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityStyleType {
    ACTIVE("시간내서 적극적으로"),
    CASUAL("일상 안에서 부담없이");

    private final String label;
}
