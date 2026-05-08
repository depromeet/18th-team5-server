package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {

    EXPLORER("자연 탐험가", "밖에서 적극적으로 활동하는 타입"),
    WALKER("동네 산책러", "밖에서 부담 없이 활동하는 타입"),
    LIFE_CREATOR("라이프 크리에이터", "실내에서 적극적으로 활동하는 타입"),
    AESTHETE("일상 감각자", "실내에서 부담 없이 활동하는 타입");

    private final String label;
    private final String description;
}
