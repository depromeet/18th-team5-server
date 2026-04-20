package com.team.peektime_admin.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // 공통
    _OK("COMMON_200", "성공"),
    _CREATED("COMMON_201", "생성 성공"),

    // 미션
    MISSION_GENERATED("MISSION_201", "미션 생성 성공"),
    MISSION_UPDATED("MISSION_200", "미션 수정 성공"),
    MISSION_ASSIGNED("MISSION_200", "미션 배정 성공"),

    // 절기
    SOLAR_TERM_FOUND("SOLAR_TERM_200", "절기 조회 성공");

    private final String code;
    private final String message;
}
