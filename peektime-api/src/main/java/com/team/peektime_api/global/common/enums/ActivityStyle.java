package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityStyle {
    ACTIVE("적극적으로", "시간을 내서 적극적으로"),
    CASUAL("부담 없이", "일상 안에서 부담 없이");

    private final String label;
    private final String description;
}
