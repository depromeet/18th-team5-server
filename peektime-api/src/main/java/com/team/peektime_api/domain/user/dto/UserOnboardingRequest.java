package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.global.common.enums.ActivityStyle;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.SpaceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class UserOnboardingRequest {

    @NotNull
    private SpaceType activityLocation;

    @NotNull
    private ActivityStyle activityStyle;

    @NotNull
    @Size(min = 3, max = 3)
    @Valid
    private List<CategoryRankItem> categoryRanks;

    @Getter
    public static class CategoryRankItem {

        @NotNull
        private EnjoyType category;

        @NotNull
        @Min(1)
        @Max(3)
        private Integer rank;
    }
}
