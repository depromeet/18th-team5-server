package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryType {
    FOOD("음식", "제철 음식, 요리 관련 미션"),
    NATURE("자연", "자연 감상, 야외 활동 미션"),
    RECORD("기록", "사진, 일기 등 기록 미션"),
    PLACE("장소", "특정 장소 방문 미션"),
    SENSE("감각", "오감을 활용한 미션");

    private final String label;
    private final String description;
}


/**
 *
 * 기록 -> 컨텐츠: ENUM 이름도 바껴야함
 * 감각 -> 음악 : ENUM 이름도 바껴야한다.
 * LLM 한테 요청 받을때도 전부 수정해줘야한다.
 * 수정된 ENUM 으로 받도록 수정해야한다.
 *
 */