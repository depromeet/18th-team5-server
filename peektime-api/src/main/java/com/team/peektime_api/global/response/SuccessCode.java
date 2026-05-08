package com.team.peektime_api.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // 공통
    _OK("COMMON_200", "성공"),
    _CREATED("COMMON_201", "생성 성공"),

    // 사용자
    USER_FOUND("USER_200", "사용자 조회 성공"),
    USER_CREATED("USER_201", "사용자 생성 성공"),
    USER_UPDATED("USER_200", "사용자 정보 수정 성공"),

    // 홈
    HOME_FOUND("HOME_200", "홈 화면 조회 성공"),

    // 미션
    MISSION_FOUND("MISSION_200", "미션 조회 성공"),
    MISSION_COMPLETED("MISSION_200", "미션 완료 성공"),
    MISSION_SELECTED("MISSION_201", "미션 선택 성공"),

    // 기록
    RECORD_CREATED("RECORD_201", "기록 생성 성공"),
    RECORD_FOUND("RECORD_200", "기록 조회 성공"),

    // 인증
    LOGIN_SUCCESS("AUTH_200", "로그인 성공"),
    TOKEN_REFRESHED("AUTH_200_REFRESH", "Access Token 재발급 성공"),
    LOGOUT_SUCCESS("AUTH_204", "로그아웃 성공"),

    // S3
    S3_PRESIGNED_URL("S3_200", "Presigned URL 발급 성공"),
    S3_IMAGE_DELETED("S3_200", "이미지 삭제 성공");

    private final String code;
    private final String message;
}