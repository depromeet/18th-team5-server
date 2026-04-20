package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionType {
    DAILY("오늘의 미션", "모든 사용자에게 동일하게 제공되는 미션"),
    RECOMMENDED("추천 미션", "사용자 타입별로 제공되는 맞춤 미션");

    private final String label;
    private final String description;
}
