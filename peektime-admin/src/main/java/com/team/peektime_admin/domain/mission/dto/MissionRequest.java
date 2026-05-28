package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.global.common.enums.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MissionRequest {

    private Long id;
    private String title;
    private String description;
    private SpaceType spaceType;
    private CompanionType companionType;
    private CategoryType categoryType;
    private EnjoyType enjoyType;
    private UserType userType;
}