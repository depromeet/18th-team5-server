package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionStatus {
    POOL("미션풀", "LLM이 생성한 미션이 저장되는 풀"),
    PENDING("배정대기", "미션풀에서 선택되어 배정을 기다리는 상태"),
    ASSIGNED("배정완료", "특정 날짜에 배정이 완료된 상태");

    private final String label;
    private final String description;
}
