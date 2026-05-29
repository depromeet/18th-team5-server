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
    ONBOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_ONBOARD", "온보딩 정보를 찾을 수 없습니다"),

    // 미션
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_404", "미션을 찾을 수 없습니다"),
    MISSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "MISSION_409", "이미 완료한 미션입니다"),
    MISSION_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "MISSION_400", "선택할 수 없는 미션입니다"),
    DAILY_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_404_DAILY", "오늘의 미션을 찾을 수 없습니다"),
    RECOMMENDED_MISSION_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "MISSION_409_REC", "추천 미션은 하루 3회까지만 기록할 수 있습니다"),
    SELECTED_MISSION_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "MISSION_409_SEL", "선택 미션은 하루 1회만 기록할 수 있습니다"),

    // 절기
    SOLAR_TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "SOLAR_TERM_404", "절기를 찾을 수 없습니다"),

    // 기록
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_404", "기록을 찾을 수 없습니다"),
    CALENDAR_FREE_RECORD_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "RECORD_409_FREE", "자유 기록은 하루 1개까지 등록 가능합니다"),
    CALENDAR_TOTAL_CARD_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "RECORD_409_TOTAL", "하루 최대 기록 수를 초과했습니다"),

    // 인증
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_EXPIRED", "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_401_RT", "Refresh Token이 존재하지 않습니다"),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_401_MISMATCH", "Refresh Token이 일치하지 않습니다"),

    // S3
    S3_INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "S3_400", "허용되지 않는 파일 형식입니다. (image/jpeg, image/png, image/heic)");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}