package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {
    NATURE_EXPLORER("자연 탐험가", "밖에서 적극적으로 활동하는 타입", SpaceType.OUTDOOR, IntensityType.ACTIVE),
    NEIGHBORHOOD_WALKER("동네 산책러", "밖에서 가볍게 활동하는 타입", SpaceType.OUTDOOR, IntensityType.LIGHT),
    SEASONAL_GOURMET("제철 미식가", "실내에서 적극적으로 활동하는 타입", SpaceType.INDOOR, IntensityType.ACTIVE),
    DAILY_OBSERVER("일상 관찰자", "실내에서 가볍게 활동하는 타입", SpaceType.INDOOR, IntensityType.LIGHT);

    private final String label;
    private final String description;
    private final SpaceType spaceType;
    private final IntensityType intensityType;
}
