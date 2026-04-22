package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IntensityType {
    LIGHT("가벼운", "이동 없음, 5분 이내"),
    MODERATE("보통", "동네 범위, 30분 이내"),
    ACTIVE("적극적", "이동 필요, 1시간 이상");

    private final String label;
    private final String description;
}
