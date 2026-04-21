package com.team.peektime_api.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {
    NATURE_EXPLORER("자연 탐험가", SpaceType.OUTDOOR, IntensityType.ACTIVE),
    NEIGHBORHOOD_WALKER("동네 산책러", SpaceType.OUTDOOR, IntensityType.LIGHT),
    SEASONAL_FOODIE("제철 미식가", SpaceType.INDOOR, IntensityType.ACTIVE),
    DAILY_OBSERVER("일상 관찰자", SpaceType.INDOOR, IntensityType.LIGHT);

    private final String description;
    private final SpaceType spaceType;
    private final IntensityType intensityType;
}
