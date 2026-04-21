package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionStatus {
    DRAFT("초안"), // LLM 이 생성한 후
    APPROVED("승인됨"), // 미션 승인
    ARCHIVED("보관됨");

    private final String description;
}
