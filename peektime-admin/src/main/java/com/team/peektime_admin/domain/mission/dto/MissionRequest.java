package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.domain.mission.validation.MissionTextPolicy;
import com.team.peektime_admin.global.common.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MissionRequest {

    private Long id;

    @NotBlank(message = "미션 title은 필수입니다.")
    @Size(max = MissionTextPolicy.TITLE_MAX_LENGTH, message = "미션 title은 공백 포함 {max}자 이내여야 합니다.")
    private String title;

    @Size(max = MissionTextPolicy.DESCRIPTION_MAX_LENGTH, message = "미션 description은 공백 포함 {max}자 이내여야 합니다.")
    private String description;
    private SpaceType spaceType;
    private CompanionType companionType;
    private CategoryType categoryType;
    private EnjoyType enjoyType;
    private UserType userType;
}
