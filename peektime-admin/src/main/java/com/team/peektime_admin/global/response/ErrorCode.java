package com.team.peektime_admin.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 리소스를 찾을 수 없습니다"),

    // 미션
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_404", "미션을 찾을 수 없습니다"),
    MISSION_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MISSION_500", "미션 생성에 실패했습니다"),
    MISSION_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "MISSION_409", "이미 배정된 미션입니다"),

    // 절기
    SOLAR_TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "SOLAR_TERM_404", "절기를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
