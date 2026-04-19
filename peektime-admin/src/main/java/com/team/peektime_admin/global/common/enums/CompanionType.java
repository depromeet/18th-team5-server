package com.team.peektime_admin.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanionType {
    SOLO("혼자 가능"),
    TOGETHER("같이하면 더 좋음");

    private final String description;
}
