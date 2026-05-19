package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanionType {
    SOLO("혼자 가능", "혼자", "혼자서도 충분히 즐길 수 있는 미션"),
    TOGETHER("같이하면 더 좋음", "같이", "함께하면 더 재미있는 미션");

    private final String label;
    private final String shortLabel;
    private final String description;
}
