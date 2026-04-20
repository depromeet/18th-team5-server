package com.team.peektime_api.global.response;

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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다"),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409", "이미 존재하는 사용자입니다"),

    // 미션
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_404", "미션을 찾을 수 없습니다"),
    MISSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "MISSION_409", "이미 완료한 미션입니다"),
    MISSION_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "MISSION_400", "선택할 수 없는 미션입니다"),

    // 기록
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_404", "기록을 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}