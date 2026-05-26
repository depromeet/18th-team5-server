package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.global.common.enums.UserType;

import java.util.List;

public interface MissionRepositoryCustom {

    List<Mission> findSelectedMissions(Long userId, Long solarTermId, UserType userType, SelectedMissionRequest filter);
}